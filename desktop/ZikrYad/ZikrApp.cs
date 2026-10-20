using System;
using System.Threading;
using System.Windows;
using System.Windows.Threading;
using Microsoft.Win32;
using ZikrYad.Core;
using ZikrYad.Views;
using WinForms = System.Windows.Forms;

namespace ZikrYad
{
    /// <summary>
    /// The app's heart: the name of Allah Ta'ala in the taskbar's notification area, the rotating dhikr,
    /// the floating tasbih, the unlock dhikr and the reminder.
    /// </summary>
    public static class ZikrApp
    {
        public static Settings S { get; private set; }
        private static WinForms.NotifyIcon _tray;
        private static TasbihWindow _tasbih;
        private static EdgeTabWindow _tab;
        private static MainWindow _main;
        private static readonly DispatcherTimer Rotate = new DispatcherTimer();
        private static readonly DispatcherTimer Remind = new DispatcherTimer { Interval = TimeSpan.FromMinutes(1) };
        private static readonly DispatcherTimer SaveTimer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(800) };
        private static EventWaitHandle _showSignal;
        private static bool _toldBackground;

        public static void Start(bool startHidden)
        {
            S = Settings.Load();
            L.Lang = S.Lang;
            if (S.LastZikr == DateTime.MinValue) S.LastZikr = DateTime.Now;

            if (S.FirstRun)
            {
                S.FirstRun = false;
                SetAutostart(S.Autostart);
            }

            CreateTray();
            SaveTimer.Tick += (s, e) => { SaveTimer.Stop(); S.Save(); };
            Rotate.Tick += (s, e) => NextDhikr(true);
            RestartRotation();
            Remind.Tick += (s, e) => CheckReminder();
            Remind.Start();
            SystemEvents.SessionSwitch += OnSessionSwitch;
            ListenForSecondCopy();
            SyncOverlay();
            if (!startHidden) ShowMain();
            S.Save();
        }

        public static void Stop()
        {
            try
            {
                SystemEvents.SessionSwitch -= OnSessionSwitch;
                S?.Save();
                if (_tray != null) { _tray.Visible = false; _tray.Dispose(); }
            }
            catch (Exception) { }
        }

        public static void Exit()
        {
            Stop();
            Application.Current.Shutdown();
        }

        public static void SaveSoon()
        {
            SaveTimer.Stop();
            SaveTimer.Start();
        }

        // ------------------------------------------------ tray
        private static void CreateTray()
        {
            System.Drawing.Icon icon;
            var res = Application.GetResourceStream(new Uri("pack://application:,,,/Assets/tray.ico"));
            using (var st = res.Stream) icon = new System.Drawing.Icon(st, WinForms.SystemInformation.SmallIconSize);
            _tray = new WinForms.NotifyIcon { Icon = icon, Visible = true };
            _tray.MouseClick += (s, e) => { if (e.Button == WinForms.MouseButtons.Left) ShowMain(); };
            BuildTrayMenu();
            UpdateTrayText();
        }

        private static void BuildTrayMenu()
        {
            var m = new WinForms.ContextMenuStrip { RightToLeft = L.Rtl ? WinForms.RightToLeft.Yes : WinForms.RightToLeft.No };
            m.Font = new System.Drawing.Font("Segoe UI", 10f);
            m.Items.Add(L.T("m_open"), null, (s, e) => ShowMain());
            m.Items.Add(S.Overlay ? L.T("tas_hide") : L.T("tas_show"), null, (s, e) => SetOverlay(!S.Overlay));
            m.Items.Add(L.T("m_next"), null, (s, e) => NextDhikr(true));
            m.Items.Add(new WinForms.ToolStripSeparator());
            m.Items.Add(L.T("m_exit"), null, (s, e) => Exit());
            var old = _tray.ContextMenuStrip;
            _tray.ContextMenuStrip = m;
            old?.Dispose();
        }

        private static void UpdateTrayText()
        {
            var t = "ﷲ · " + S.Current();
            _tray.Text = t.Length > 120 ? t.Substring(0, 117) + "…" : t;
        }

        // ------------------------------------------------ dhikr rotation
        public static void RestartRotation()
        {
            Rotate.Stop();
            Rotate.Interval = TimeSpan.FromMinutes(Math.Max(1, S.IntervalMinutes));
            Rotate.Start();
        }

        public static void NextDhikr(bool show)
        {
            S.Next();
            UpdateTrayText();
            SaveSoon();
            _main?.RefreshState();
            if (show && S.PopupDhikr) PopupCard.Show(S.Current(), null, true, 5000);
        }

        // ------------------------------------------------ tasbih
        public static void SetOverlay(bool on)
        {
            S.Overlay = on;
            if (on) S.Mini = 0;
            SaveSoon();
            SyncOverlay();
            BuildTrayMenu();
            _main?.RefreshState();
        }

        public static void SyncOverlay()
        {
            if (!S.Overlay)
            {
                _tasbih?.Close(); _tasbih = null;
                _tab?.Close(); _tab = null;
                return;
            }
            if (S.Mini != 0)
            {
                _tasbih?.Close(); _tasbih = null;
                if (_tab == null) { _tab = new EdgeTabWindow(S.Mini); _tab.Show(); }
            }
            else
            {
                _tab?.Close(); _tab = null;
                if (_tasbih == null) { _tasbih = new TasbihWindow(); _tasbih.Show(); }
                _tasbih.Refresh();
            }
        }

        public static void RefreshTasbih()
        {
            _tasbih?.Refresh();
        }

        public static void CountOne()
        {
            S.Count++;
            S.LastZikr = DateTime.Now;
            SaveSoon();
            _tasbih?.Refresh();
            _main?.RefreshState();
            if (S.Target > 0 && S.Count % S.Target == 0) System.Media.SystemSounds.Asterisk.Play();
        }

        public static void ResetCount()
        {
            S.Count = 0;
            SaveSoon();
            _tasbih?.Refresh();
            _main?.RefreshState();
        }

        // ------------------------------------------------ unlock and reminder
        private static void OnSessionSwitch(object sender, SessionSwitchEventArgs e)
        {
            if (e.Reason != SessionSwitchReason.SessionUnlock || !S.UnlockPopup) return;
            Application.Current.Dispatcher.BeginInvoke(new Action(() =>
                PopupCard.Show(S.NextUnlockText(), "فَاذْكُرُونِي أَذْكُرْكُمْ", false, 2000)));
        }

        private static void CheckReminder()
        {
            int hours = S.ReminderHours;
            if (hours <= 0) return;
            var now = DateTime.Now;
            var gap = TimeSpan.FromHours(hours);
            if (now - S.LastZikr < gap || now - S.LastReminder < gap) return;
            if (now.Hour >= 21 || now.Hour < 6) return; // quiet at night
            S.LastReminder = now;
            SaveSoon();
            string body = hours == 1 ? L.T("rem_1h") : L.F("rem_nh", L.Num(hours));
            PopupCard.Show("فَاذْكُرُونِي أَذْكُرْكُمْ", body, true, 12000, () => SetOverlay(true));
        }

        // ------------------------------------------------ windows
        public static void ShowMain()
        {
            if (_main == null)
            {
                var w = new MainWindow();
                _main = w;
                _main.Closed += (s, e) =>
                {
                    if (_main == w) _main = null;
                    if (!_toldBackground)
                    {
                        _toldBackground = true;
                        _tray.ShowBalloonTip(4000, L.T("app_name"), L.T("running_bg"), WinForms.ToolTipIcon.None);
                    }
                };
                _main.Show();
            }
            if (_main.WindowState == WindowState.Minimized) _main.WindowState = WindowState.Normal;
            _main.Activate();
        }

        public static void LanguageChanged()
        {
            L.Lang = S.Lang;
            SaveSoon();
            BuildTrayMenu();
            _tasbih?.Rebuild();
            if (_main != null)
            {
                var old = _main;
                _main = null;
                var w = new MainWindow { Left = old.Left, Top = old.Top, Width = old.Width, Height = old.Height };
                _main = w;
                w.Closed += (s, e) => { if (_main == w) _main = null; };
                w.Show();
                old.Close();
            }
        }

        private static void ListenForSecondCopy()
        {
            _showSignal = new EventWaitHandle(false, EventResetMode.AutoReset, "ZikrYad.Show.Ghora");
            var t = new Thread(() =>
            {
                while (true)
                {
                    _showSignal.WaitOne();
                    Application.Current?.Dispatcher.BeginInvoke(new Action(ShowMain));
                }
            }) { IsBackground = true };
            t.Start();
        }

        // ------------------------------------------------ start with Windows
        public static void SetAutostart(bool on)
        {
            try
            {
                using (var key = Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Run", true))
                {
                    if (key == null) return;
                    if (on)
                    {
                        var exe = Environment.ProcessPath ?? System.Diagnostics.Process.GetCurrentProcess().MainModule.FileName;
                        key.SetValue("ZikrYad", "\"" + exe + "\" --tray");
                    }
                    else key.DeleteValue("ZikrYad", false);
                }
            }
            catch (Exception) { }
        }
    }
}

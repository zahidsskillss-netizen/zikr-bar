using System;
using System.Collections.Generic;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Effects;
using ZikrYad.Core;

namespace ZikrYad.Views
{
    /// <summary>The settings window: same sections as the Android app.</summary>
    public class MainWindow : Window
    {
        private const string Ayah = "فَاذْكُرُونِي أَذْكُرْكُمْ";
        private Settings S => ZikrApp.S;
        private bool _binding;

        private Toggle _popupToggle, _tasbihToggle, _unlockToggle, _autoToggle;
        private TextBlock _current, _count, _statBar, _statCount, _statGoal;
        private ComboBox _interval, _tasZikr, _target, _reminder;
        private TextBox _list;
        private readonly List<Border> _tiles = new List<Border>();

        public MainWindow()
        {
            Title = L.T("app_name");
            Width = 560;
            Height = 860;
            MinWidth = 440;
            MinHeight = 520;
            WindowStartupLocation = WindowStartupLocation.CenterScreen;
            Background = Theme.B(Theme.Bg);
            Icon = Theme.Image("app.ico");
            FlowDirection = L.Rtl ? FlowDirection.RightToLeft : FlowDirection.LeftToRight;
            UseLayoutRounding = true;
            TextOptions.SetTextFormattingMode(this, TextFormattingMode.Ideal);

            var root = new StackPanel { Margin = new Thickness(0, 0, 0, 28) };
            root.Children.Add(Header());
            root.Children.Add(Stats());
            TrayCard(root);
            TasbihCard(root);
            UnlockCard(root);
            ReminderCard(root);
            StartupCard(root);
            ListCard(root);
            MoreCard(root);
            var credit = Ui.Text(L.T("credit"), 13, Theme.Muted);
            credit.TextAlignment = TextAlignment.Center;
            credit.Margin = new Thickness(16, 22, 16, 0);
            root.Children.Add(credit);

            Content = new ScrollViewer { Content = root, VerticalScrollBarVisibility = ScrollBarVisibility.Auto };
            RefreshState();
        }

        // ------------------------------------------------ header
        private UIElement Header()
        {
            var h = new StackPanel { HorizontalAlignment = HorizontalAlignment.Stretch };
            var top = new DockPanel { LastChildFill = false };
            var lang = Ui.Combo(L.Names, false);
            lang.Width = 140;
            lang.SelectedIndex = L.Lang;
            lang.SelectionChanged += (s, e) =>
            {
                if (_binding || lang.SelectedIndex < 0 || lang.SelectedIndex == S.Lang) return;
                S.Lang = lang.SelectedIndex;
                ZikrApp.LanguageChanged();
            };
            DockPanel.SetDock(lang, Dock.Left);
            top.Children.Add(lang);
            var share = Ui.GoldButton("↗  " + L.T("share_now"), () => InfoWindow.Share(this));
            share.Margin = new Thickness(0);
            DockPanel.SetDock(share, Dock.Right);
            top.Children.Add(share);
            h.Children.Add(top);

            var logo = new Image { Source = Theme.Image("logo.png"), Width = 96, Height = 96, Margin = new Thickness(0, 16, 0, 0), FlowDirection = FlowDirection.LeftToRight };
            logo.Effect = new DropShadowEffect { BlurRadius = 26, ShadowDepth = 6, Opacity = 0.45, Direction = 270 };
            h.Children.Add(logo);
            var title = Ui.Title(L.T("app_name"), 34, Colors.White);
            title.HorizontalAlignment = HorizontalAlignment.Center;
            title.Margin = new Thickness(0, 10, 0, 2);
            h.Children.Add(title);
            h.Children.Add(Ui.Arabic(Ayah, 19, Color.FromArgb(0xE6, 0xFF, 0xFF, 0xFF)));

            return new Border
            {
                Background = Theme.GreenGradient(), CornerRadius = new CornerRadius(0, 0, 32, 32),
                Padding = new Thickness(18, 14, 18, 58), Child = h
            };
        }

        private UIElement Stats()
        {
            var g = new Grid();
            for (int i = 0; i < 5; i++) g.ColumnDefinitions.Add(new ColumnDefinition { Width = i % 2 == 0 ? new GridLength(1, GridUnitType.Star) : GridLength.Auto });
            _statBar = StatCell(g, 0, L.T("stat_status"));
            Divider(g, 1);
            _statCount = StatCell(g, 2, L.T("stat_count"));
            Divider(g, 3);
            _statGoal = StatCell(g, 4, L.T("stat_goal"));
            return new Border
            {
                Background = Brushes.White, CornerRadius = new CornerRadius(22), BorderBrush = Theme.B(Theme.Line), BorderThickness = new Thickness(1),
                Margin = new Thickness(20, -40, 20, 0), Padding = new Thickness(8, 12, 8, 12), Child = g,
                Effect = new DropShadowEffect { BlurRadius = 18, ShadowDepth = 4, Opacity = 0.14, Direction = 270 }
            };
        }

        private TextBlock StatCell(Grid g, int col, string label)
        {
            var sp = new StackPanel { HorizontalAlignment = HorizontalAlignment.Center };
            var v = Ui.Title("", 20, Theme.Green);
            v.HorizontalAlignment = HorizontalAlignment.Center;
            sp.Children.Add(v);
            var l = Ui.Strong(label, 12, Theme.Muted);
            l.HorizontalAlignment = HorizontalAlignment.Center;
            sp.Children.Add(l);
            Grid.SetColumn(sp, col);
            g.Children.Add(sp);
            return v;
        }

        private static void Divider(Grid g, int col)
        {
            var d = new Border { Width = 1, Height = 36, Background = Theme.B(Theme.Line), VerticalAlignment = VerticalAlignment.Center };
            Grid.SetColumn(d, col);
            g.Children.Add(d);
        }

        private UIElement SwitchRow(string label, Toggle t)
        {
            var row = new DockPanel { LastChildFill = true };
            DockPanel.SetDock(t, Dock.Right);
            row.Children.Add(t);
            var l = Ui.Title(label, 15, Theme.Ink);
            l.VerticalAlignment = VerticalAlignment.Center;
            row.Children.Add(l);
            var b = Ui.Soft(row, 10);
            b.Margin = new Thickness(0, 8, 0, 0);
            return b;
        }

        // ------------------------------------------------ cards
        private void TrayCard(StackPanel root)
        {
            var c = Ui.Card(root, "ﷲ", L.T("c_tray_t"), L.T("c_tray_s"));
            _popupToggle = new Toggle();
            _popupToggle.Changed += on => { S.PopupDhikr = on; ZikrApp.SaveSoon(); };
            c.Children.Add(SwitchRow(L.T("popup_dhikr"), _popupToggle));

            c.Children.Add(Ui.Label(L.T("current_zikr")));
            _current = Ui.Arabic("", 20, Theme.Green);
            c.Children.Add(Ui.Soft(_current));
            var next = Ui.Button(L.T("m_next"), false, () => ZikrApp.NextDhikr(true));
            c.Children.Add(next);

            c.Children.Add(Ui.Label(L.T("interval")));
            var labels = new List<string>();
            foreach (var m in Settings.Intervals) labels.Add(m == 1 ? L.T("minute1") : L.F("minutes", L.Num(m)));
            _interval = Ui.Combo(labels.ToArray(), false);
            _interval.SelectionChanged += (s, e) =>
            {
                if (_binding || _interval.SelectedIndex < 0) return;
                S.IntervalMinutes = Settings.Intervals[_interval.SelectedIndex];
                ZikrApp.SaveSoon();
                ZikrApp.RestartRotation();
            };
            c.Children.Add(_interval);
        }

        private void TasbihCard(StackPanel root)
        {
            var c = Ui.Card(root, L.Num(33), L.T("c_tas_t"), L.T("c_tas_s_pc"));
            _tasbihToggle = new Toggle();
            _tasbihToggle.Changed += on => ZikrApp.SetOverlay(on);
            c.Children.Add(SwitchRow(L.T("enabled"), _tasbihToggle));

            c.Children.Add(Ui.Label(L.T("choose_design")));
            var wrap = new WrapPanel();
            for (int i = 0; i < TasbihVisual.Count; i++) wrap.Children.Add(Tile(i));
            c.Children.Add(wrap);

            c.Children.Add(Ui.Label(L.T("tas_zikr")));
            _tasZikr = Ui.Combo(new string[0], true);
            _tasZikr.SelectionChanged += (s, e) =>
            {
                if (_binding || _tasZikr.SelectedIndex < 0 || _tasZikr.SelectedIndex == S.TasbihIdx()) return;
                S.TasbihIndex = _tasZikr.SelectedIndex;
                S.Count = 0;
                ZikrApp.SaveSoon();
                ZikrApp.RefreshTasbih();
                RefreshState();
            };
            c.Children.Add(_tasZikr);

            c.Children.Add(Ui.Label(L.T("goal_label")));
            var tl = new List<string>();
            foreach (var t in Settings.Targets) tl.Add(t == 0 ? L.T("unlimited") : L.F("times", L.Num(t)));
            _target = Ui.Combo(tl.ToArray(), false);
            _target.SelectionChanged += (s, e) =>
            {
                if (_binding || _target.SelectedIndex < 0) return;
                S.Target = Settings.Targets[_target.SelectedIndex];
                ZikrApp.SaveSoon();
                ZikrApp.RefreshTasbih();
                RefreshState();
            };
            c.Children.Add(_target);

            var countRow = new DockPanel();
            var reset = Ui.Button(L.T("reset"), false, () => ZikrApp.ResetCount());
            reset.Margin = new Thickness(0);
            DockPanel.SetDock(reset, Dock.Right);
            countRow.Children.Add(reset);
            var cl = Ui.Title(L.T("count"), 15, Theme.Ink);
            cl.VerticalAlignment = VerticalAlignment.Center;
            DockPanel.SetDock(cl, Dock.Left);
            countRow.Children.Add(cl);
            _count = Ui.Title("0", 30, Theme.Green);
            _count.Margin = new Thickness(12, 0, 12, 0);
            _count.VerticalAlignment = VerticalAlignment.Center;
            countRow.Children.Add(_count);
            var cb = Ui.Soft(countRow, 10);
            cb.Margin = new Thickness(0, 16, 0, 0);
            c.Children.Add(cb);

            c.Children.Add(Ui.Button(L.T("orig_size"), false, () =>
            {
                S.Scale = 1.0;
                ZikrApp.SaveSoon();
                ZikrApp.RefreshTasbih();
            }));

            var how = new Expander
            {
                Header = Ui.Strong(L.T("howto"), 14.5, Theme.Green), Margin = new Thickness(0, 14, 0, 0), IsExpanded = false
            };
            var hs = new StackPanel();
            string[,] rows =
            {
                { "dh_click_k", "h_tap_v" }, { "h_drag_k", "h_drag_v" }, { "dh_wheel_k", "dh_wheel_v" },
                { "h_edge_k", "h_edge_v" }, { "dh_right_k", "dh_right_v" }, { "h_long_k", "h_long_v" }
            };
            for (int i = 0; i < rows.GetLength(0); i++) hs.Children.Add(HowRow(L.T(rows[i, 0]), L.T(rows[i, 1])));
            how.Content = hs;
            c.Children.Add(how);
        }

        private UIElement HowRow(string key, string desc)
        {
            var g = new Grid { Margin = new Thickness(0, 4, 0, 4) };
            g.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(110) });
            g.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            var k = Ui.Strong(key, 12.5, Theme.Green);
            k.TextAlignment = TextAlignment.Center;
            var kb = new Border { Background = Theme.B(0x14, Theme.Green), CornerRadius = new CornerRadius(10), Padding = new Thickness(6, 3, 6, 5), Child = k, VerticalAlignment = VerticalAlignment.Top };
            g.Children.Add(kb);
            var d = Ui.Text(desc, 13.5, Theme.Muted);
            d.Margin = new Thickness(10, 1, 6, 0);
            Grid.SetColumn(d, 1);
            g.Children.Add(d);
            return g;
        }

        private Border Tile(int style)
        {
            var v = new TasbihVisual { FlowDirection = FlowDirection.LeftToRight };
            v.SetState(style, "سُبْحَانَ اللَّهِ", 12, 33);
            var box = new Border
            {
                Width = 130, Height = 150, CornerRadius = new CornerRadius(16), ClipToBounds = true,
                Background = new LinearGradientBrush(Color.FromRgb(0xE9, 0xEE, 0xF3), Color.FromRgb(0xCF, 0xD8, 0xE2), 45),
                Child = new Viewbox { Child = v, Stretch = Stretch.Uniform, StretchDirection = StretchDirection.DownOnly, Margin = new Thickness(6) }
            };
            var name = Ui.Strong(L.Num(style + 1) + " · " + TasbihVisual.Name(style), 12.5, Theme.Ink);
            name.HorizontalAlignment = HorizontalAlignment.Center;
            name.Margin = new Thickness(0, 6, 0, 0);
            var sp = new StackPanel();
            sp.Children.Add(box);
            sp.Children.Add(name);
            var t = new Border { Child = sp, Padding = new Thickness(6), Margin = new Thickness(0, 0, 8, 8), CornerRadius = new CornerRadius(20), BorderThickness = new Thickness(2), Cursor = Cursors.Hand };
            t.MouseLeftButtonUp += (s, e) =>
            {
                S.TasbihStyle = style;
                ZikrApp.SaveSoon();
                ZikrApp.RefreshTasbih();
                RefreshState();
            };
            _tiles.Add(t);
            return t;
        }

        private void UnlockCard(StackPanel root)
        {
            var c = Ui.Card(root, "☀", L.T("c_unl_t"), L.T("c_unl_s_pc"));
            _unlockToggle = new Toggle();
            _unlockToggle.Changed += on => { S.UnlockPopup = on; ZikrApp.SaveSoon(); };
            c.Children.Add(SwitchRow(L.T("enabled"), _unlockToggle));
            var note = Ui.Text(L.T("unl_note"), 13, Theme.Muted);
            note.Margin = new Thickness(0, 10, 0, 0);
            c.Children.Add(note);
        }

        private void ReminderCard(StackPanel root)
        {
            var c = Ui.Card(root, "☾", L.T("c_rem_t"), L.T("c_rem_s"));
            var ayah = Ui.Arabic(Ayah, 22, Theme.Green);
            var ab = Ui.Soft(ayah);
            ab.Margin = new Thickness(0, 6, 0, 0);
            c.Children.Add(ab);
            c.Children.Add(Ui.Label(L.T("after_hours")));
            var labels = new List<string>();
            foreach (var h in Settings.ReminderHoursList) labels.Add(h == 0 ? L.T("rem_off") : h == 1 ? L.T("hour1") : L.F("hours", L.Num(h)));
            _reminder = Ui.Combo(labels.ToArray(), false);
            _reminder.SelectionChanged += (s, e) =>
            {
                if (_binding || _reminder.SelectedIndex < 0) return;
                S.ReminderHours = Settings.ReminderHoursList[_reminder.SelectedIndex];
                S.LastReminder = DateTime.MinValue;
                ZikrApp.SaveSoon();
            };
            c.Children.Add(_reminder);
            var note = Ui.Text(L.T("quiet"), 13, Theme.Muted);
            note.Margin = new Thickness(0, 10, 0, 0);
            c.Children.Add(note);
        }

        private void StartupCard(StackPanel root)
        {
            var c = Ui.Card(root, "⏻", L.T("autostart_t"), L.T("autostart_s"));
            _autoToggle = new Toggle();
            _autoToggle.Changed += on => { S.Autostart = on; ZikrApp.SetAutostart(on); ZikrApp.SaveSoon(); };
            c.Children.Add(SwitchRow(L.T("enabled"), _autoToggle));
        }

        private void ListCard(StackPanel root)
        {
            var c = Ui.Card(root, "≡", L.T("c_list_t"), L.T("c_list_s"));
            _list = new TextBox
            {
                Text = S.RawList, FontFamily = Theme.Amiri, FontWeight = FontWeights.Bold, FontSize = 18, AcceptsReturn = true, TextWrapping = TextWrapping.Wrap,
                MinHeight = 160, MaxHeight = 320, VerticalScrollBarVisibility = ScrollBarVisibility.Auto, FlowDirection = FlowDirection.RightToLeft,
                Padding = new Thickness(10), BorderBrush = Theme.B(Theme.Line), Background = Theme.B(Theme.Soft), Margin = new Thickness(0, 10, 0, 0)
            };
            c.Children.Add(_list);
            c.Children.Add(Ui.Button(L.T("save"), true, () =>
            {
                S.SaveList(_list.Text.Replace("\r", ""));
                ZikrApp.SaveSoon();
                ZikrApp.RefreshTasbih();
                RefreshState();
                PopupCard.Show(L.T("saved"), null, true, 1800);
            }));
        }

        private void MoreCard(StackPanel root)
        {
            var c = Ui.Card(root, "✦", L.T("c_more_t"), L.T("c_more_s"));
            c.Children.Add(NavRow(Ui.Badge("♥", 46), L.T("nav_donate"), "HesabPay · Atoma Pay · Payoneer", InfoWindow.PageDonate));
            c.Children.Add(NavRow(Ui.Badge("ﷲ", 46), L.T("nav_about"), L.T("nav_about_s"), InfoWindow.PageAbout));
            c.Children.Add(NavRow(Ui.Photo(46, false), L.T("nav_dev"), L.T("dev_name") + " · " + L.T("ghora"), InfoWindow.PageDev));
            c.Children.Add(Ui.Button(L.T("share_btn"), true, () => InfoWindow.Share(this)));
        }

        private UIElement NavRow(FrameworkElement icon, string title, string sub, int page)
        {
            var row = new DockPanel { Margin = new Thickness(0, 6, 0, 6), Background = Brushes.Transparent, Cursor = Cursors.Hand };
            DockPanel.SetDock(icon, Dock.Left);
            row.Children.Add(icon);
            var arrow = Ui.Text(L.Rtl ? "‹" : "›", 24, Theme.Muted);
            arrow.FlowDirection = FlowDirection.LeftToRight;
            arrow.VerticalAlignment = VerticalAlignment.Center;
            DockPanel.SetDock(arrow, Dock.Right);
            row.Children.Add(arrow);
            var tx = new StackPanel { Margin = new Thickness(12, 0, 8, 0), VerticalAlignment = VerticalAlignment.Center };
            tx.Children.Add(Ui.Title(title, 15.5, Theme.Ink));
            tx.Children.Add(Ui.Text(sub, 12.5, Theme.Muted));
            row.Children.Add(tx);
            row.MouseLeftButtonUp += (s, e) => new InfoWindow(page) { Owner = this }.Show();
            return row;
        }

        // ------------------------------------------------ state
        public void RefreshState()
        {
            _binding = true;
            _popupToggle.SetOn(S.PopupDhikr);
            _tasbihToggle.SetOn(S.Overlay);
            _unlockToggle.SetOn(S.UnlockPopup);
            _autoToggle.SetOn(S.Autostart);
            _current.Text = S.Current();
            _count.Text = L.Num(S.Count);
            _statBar.Text = L.T("on");
            _statCount.Text = L.Num(S.Count);
            _statGoal.Text = S.Target == 0 ? "∞" : L.Num(S.Target);
            _interval.SelectedIndex = Array.IndexOf(Settings.Intervals, S.IntervalMinutes);
            _target.SelectedIndex = Array.IndexOf(Settings.Targets, S.Target);
            _reminder.SelectedIndex = Array.IndexOf(Settings.ReminderHoursList, S.ReminderHours);
            _tasZikr.Items.Clear();
            foreach (var z in S.List()) _tasZikr.Items.Add(z);
            _tasZikr.SelectedIndex = S.TasbihIdx();
            for (int i = 0; i < _tiles.Count; i++)
            {
                bool sel = i == S.TasbihStyle;
                _tiles[i].BorderBrush = sel ? Theme.B(Theme.Green) : Brushes.Transparent;
                _tiles[i].Background = sel ? Theme.B(0x14, Theme.Green) : Brushes.Transparent;
            }
            _binding = false;
        }
    }
}

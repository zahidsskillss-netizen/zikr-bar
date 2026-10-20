using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Threading;
using ZikrYad.Core;

namespace ZikrYad.Views
{
    /// <summary>The floating tasbih: always on top, click to count, drag to move, wheel to resize.</summary>
    public class TasbihWindow : Window
    {
        private readonly TasbihVisual _v = new TasbihVisual { FlowDirection = FlowDirection.LeftToRight };
        private readonly DispatcherTimer _long = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(700) };
        private Point _down;
        private bool _pressed, _moved, _longDone;
        private Settings S => ZikrApp.S;

        public TasbihWindow()
        {
            WindowStyle = WindowStyle.None;
            AllowsTransparency = true;
            Background = Brushes.Transparent;
            Topmost = true;
            ShowInTaskbar = false;
            ShowActivated = false;
            ResizeMode = ResizeMode.NoResize;
            SizeToContent = SizeToContent.WidthAndHeight;
            Cursor = Cursors.Hand;
            Content = _v;
            Title = "Zikr Yad · Tasbih";

            _long.Tick += (s, e) =>
            {
                _long.Stop();
                if (!_pressed || _moved) return;
                _longDone = true;
                ZikrApp.ResetCount();
                Pulse(0.9);
            };

            MouseLeftButtonDown += OnDown;
            MouseMove += OnMove;
            MouseLeftButtonUp += OnUp;
            MouseWheel += (s, e) =>
            {
                double sc = Math.Max(0.5, Math.Min(2.0, _v.Scale + (e.Delta > 0 ? 0.08 : -0.08)));
                _v.SetSize(sc);
                S.Scale = sc;
                ZikrApp.SaveSoon();
            };
            Loaded += (s, e) => PlaceStart();
            BuildMenu();
            Refresh();
        }

        private void BuildMenu()
        {
            var m = new ContextMenu { FlowDirection = L.Rtl ? FlowDirection.RightToLeft : FlowDirection.LeftToRight, FontFamily = Theme.Body, FontSize = 14 };
            void Add(string text, Action a)
            {
                var it = new MenuItem { Header = text };
                it.Click += (s, e) => a();
                m.Items.Add(it);
            }
            Add(L.T("reset"), () => ZikrApp.ResetCount());
            Add(L.T("m_next"), () => { S.TasbihIndex = S.TasbihIdx() + 1; S.Count = 0; ZikrApp.SaveSoon(); Refresh(); });
            Add(L.T("orig_size"), () => { S.Scale = 1.0; ZikrApp.SaveSoon(); Refresh(); });
            m.Items.Add(new Separator());
            Add(L.T("m_hide_edge"), () => Minimize(Left + ActualWidth / 2 < SystemParameters.VirtualScreenLeft + SystemParameters.VirtualScreenWidth / 2 ? 1 : 2));
            Add(L.T("m_close_tas"), () => ZikrApp.SetOverlay(false));
            m.Items.Add(new Separator());
            Add(L.T("m_open"), () => ZikrApp.ShowMain());
            ContextMenu = m;
        }

        public void Refresh()
        {
            _v.SetState(S.TasbihStyle, S.TasbihText(), S.Count, S.Target);
            _v.SetSize(S.Scale);
        }

        public void Rebuild()
        {
            BuildMenu();
            Refresh();
        }

        private void PlaceStart()
        {
            var wa = SystemParameters.WorkArea;
            if (double.IsNaN(S.PosX) || double.IsNaN(S.PosY))
            {
                Left = wa.Right - ActualWidth - 40;
                Top = wa.Bottom - ActualHeight - 60;
            }
            else
            {
                Left = Math.Max(SystemParameters.VirtualScreenLeft, Math.Min(S.PosX, SystemParameters.VirtualScreenLeft + SystemParameters.VirtualScreenWidth - 60));
                Top = Math.Max(SystemParameters.VirtualScreenTop, Math.Min(S.PosY, SystemParameters.VirtualScreenTop + SystemParameters.VirtualScreenHeight - 60));
            }
        }

        private void OnDown(object sender, MouseButtonEventArgs e)
        {
            _down = PointToScreen(e.GetPosition(this));
            _pressed = true;
            _moved = false;
            _longDone = false;
            _long.Start();
        }

        private void OnMove(object sender, MouseEventArgs e)
        {
            if (!_pressed || e.LeftButton != MouseButtonState.Pressed) return;
            var p = PointToScreen(e.GetPosition(this));
            if (!_moved && (Math.Abs(p.X - _down.X) > 6 || Math.Abs(p.Y - _down.Y) > 6))
            {
                _moved = true;
                _long.Stop();
                try { DragMove(); } catch (InvalidOperationException) { }
                _pressed = false;
                AfterDrag();
            }
        }

        private void OnUp(object sender, MouseButtonEventArgs e)
        {
            _long.Stop();
            if (_pressed && !_moved && !_longDone)
            {
                ZikrApp.CountOne();
                Pulse(1.06);
            }
            _pressed = false;
        }

        private void AfterDrag()
        {
            double left = SystemParameters.VirtualScreenLeft, right = left + SystemParameters.VirtualScreenWidth;
            if (Left <= left + 2) { Minimize(1); return; }
            if (Left + ActualWidth >= right - 2) { Minimize(2); return; }
            S.PosX = Left;
            S.PosY = Top;
            ZikrApp.SaveSoon();
        }

        private void Minimize(int side)
        {
            S.PosX = Math.Max(SystemParameters.VirtualScreenLeft, Math.Min(Left, SystemParameters.VirtualScreenLeft + SystemParameters.VirtualScreenWidth - ActualWidth));
            S.PosY = Top;
            S.Mini = side;
            S.MiniY = Top + ActualHeight / 2;
            ZikrApp.SaveSoon();
            ZikrApp.SyncOverlay();
        }

        private void Pulse(double to)
        {
            var sc = new ScaleTransform(1, 1, ActualWidth / 2, ActualHeight / 2);
            _v.RenderTransform = sc;
            var a = new DoubleAnimation(to, TimeSpan.FromMilliseconds(70)) { AutoReverse = true };
            sc.BeginAnimation(ScaleTransform.ScaleXProperty, a);
            sc.BeginAnimation(ScaleTransform.ScaleYProperty, a);
        }
    }

    /// <summary>The small tab left at the screen edge when the tasbih is hidden.</summary>
    public class EdgeTabWindow : Window
    {
        private Point _down;
        private bool _pressed, _moved;
        private readonly int _side;

        public EdgeTabWindow(int side)
        {
            _side = side;
            WindowStyle = WindowStyle.None;
            AllowsTransparency = true;
            Background = Brushes.Transparent;
            Topmost = true;
            ShowInTaskbar = false;
            ShowActivated = false;
            ResizeMode = ResizeMode.NoResize;
            Width = 34;
            Height = 96;
            Cursor = Cursors.Hand;
            ToolTip = L.T("app_name");

            var r = side == 1 ? new CornerRadius(0, 18, 18, 0) : new CornerRadius(18, 0, 0, 18);
            var stack = new StackPanel { VerticalAlignment = VerticalAlignment.Center };
            stack.Children.Add(new TextBlock { Text = "ﷲ", FontFamily = Theme.Amiri, FontWeight = FontWeights.Bold, FontSize = 18, Foreground = Theme.B(Theme.Gold), HorizontalAlignment = HorizontalAlignment.Center });
            stack.Children.Add(new TextBlock { Text = side == 1 ? "›" : "‹", FontSize = 20, FontWeight = FontWeights.Bold, Foreground = Brushes.White, HorizontalAlignment = HorizontalAlignment.Center });
            Content = new Border { Background = Theme.B(0xE6, Theme.Green), CornerRadius = r, Child = stack };

            Loaded += (s, e) =>
            {
                double vl = SystemParameters.VirtualScreenLeft, vw = SystemParameters.VirtualScreenWidth;
                Left = side == 1 ? vl : vl + vw - Width;
                Top = Math.Max(SystemParameters.VirtualScreenTop, ZikrApp.S.MiniY - Height / 2);
            };
            MouseLeftButtonDown += (s, e) => { _down = PointToScreen(e.GetPosition(this)); _pressed = true; _moved = false; };
            MouseMove += (s, e) =>
            {
                if (!_pressed || e.LeftButton != MouseButtonState.Pressed) return;
                var p = PointToScreen(e.GetPosition(this));
                if (!_moved && Math.Abs(p.Y - _down.Y) > 6)
                {
                    _moved = true;
                    try { DragMove(); } catch (InvalidOperationException) { }
                    _pressed = false;
                    double vl = SystemParameters.VirtualScreenLeft, vw = SystemParameters.VirtualScreenWidth;
                    Left = _side == 1 ? vl : vl + vw - Width;
                    ZikrApp.S.MiniY = Top + Height / 2;
                    ZikrApp.SaveSoon();
                }
            };
            MouseLeftButtonUp += (s, e) =>
            {
                if (_pressed && !_moved)
                {
                    var st = ZikrApp.S;
                    st.Mini = 0;
                    double vl = SystemParameters.VirtualScreenLeft, vw = SystemParameters.VirtualScreenWidth;
                    st.PosX = _side == 1 ? vl + 12 : vl + vw - 320;
                    st.PosY = Math.Max(SystemParameters.VirtualScreenTop, st.MiniY - 120);
                    ZikrApp.SaveSoon();
                    ZikrApp.SyncOverlay();
                }
                _pressed = false;
            };
        }
    }
}

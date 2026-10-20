using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Effects;
using System.Windows.Threading;
using ZikrYad.Core;

namespace ZikrYad.Views
{
    /// <summary>A green and gold card that shows a dhikr for a moment, then fades away.</summary>
    public class PopupCard : Window
    {
        private static PopupCard _current;
        private readonly DispatcherTimer _timer = new DispatcherTimer();
        private readonly Action _onClick;

        private PopupCard(string main, string sub, bool corner, int ms, Action onClick)
        {
            _onClick = onClick;
            WindowStyle = WindowStyle.None;
            AllowsTransparency = true;
            Background = Brushes.Transparent;
            Topmost = true;
            ShowInTaskbar = false;
            ShowActivated = false;
            ResizeMode = ResizeMode.NoResize;
            SizeToContent = SizeToContent.WidthAndHeight;

            var stack = new StackPanel { HorizontalAlignment = HorizontalAlignment.Center };
            var glyph = new TextBlock
            {
                Text = "ﷲ", FontFamily = Theme.Amiri, FontWeight = FontWeights.Bold, FontSize = corner ? 26 : 34, Foreground = Theme.B(Theme.Gold),
                HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center
            };
            double ring = corner ? 52 : 70;
            stack.Children.Add(new Border
            {
                Width = ring, Height = ring, CornerRadius = new CornerRadius(ring / 2), Background = Theme.B(0x22, Colors.Black),
                BorderBrush = Theme.B(0x99, Theme.Gold), BorderThickness = new Thickness(1.5), HorizontalAlignment = HorizontalAlignment.Center, Child = glyph
            });
            var t = Ui.Arabic(main, corner ? 22 : 27, Colors.White);
            t.Margin = new Thickness(0, 12, 0, 4);
            t.MaxWidth = corner ? 340 : 420;
            stack.Children.Add(t);
            if (!string.IsNullOrEmpty(sub))
            {
                var s = sub.Contains("أَذْكُرْكُمْ") ? Ui.Arabic(sub, 16, Color.FromArgb(0xD0, 0xF3, 0xE3, 0xA5)) : Ui.Text(sub, 14, Color.FromArgb(0xE0, 0xFF, 0xFF, 0xFF));
                s.TextAlignment = TextAlignment.Center;
                s.MaxWidth = corner ? 340 : 420;
                s.FlowDirection = L.Rtl ? FlowDirection.RightToLeft : FlowDirection.LeftToRight;
                stack.Children.Add(s);
            }
            var card = new Border
            {
                Background = Theme.GreenGradient(), BorderBrush = Theme.B(0xCC, Theme.Gold2), BorderThickness = new Thickness(1.5),
                CornerRadius = new CornerRadius(26), Padding = new Thickness(26, 20, 26, 22), Margin = new Thickness(24),
                Effect = new DropShadowEffect { BlurRadius = 30, ShadowDepth = 6, Opacity = 0.45, Direction = 270 },
                Child = stack, Cursor = System.Windows.Input.Cursors.Hand, MinWidth = corner ? 260 : 320
            };
            Content = card;
            MouseLeftButtonUp += (s, e) => { _onClick?.Invoke(); FadeOut(); };

            Loaded += (s, e) =>
            {
                var wa = SystemParameters.WorkArea;
                if (corner)
                {
                    Left = wa.Right - ActualWidth - 4;
                    Top = wa.Bottom - ActualHeight - 4;
                }
                else
                {
                    Left = wa.Left + (wa.Width - ActualWidth) / 2;
                    Top = wa.Top + (wa.Height - ActualHeight) / 2;
                }
                Opacity = 0;
                BeginAnimation(OpacityProperty, new DoubleAnimation(1, TimeSpan.FromMilliseconds(220)));
                var sc = new ScaleTransform(0.92, 0.92, ActualWidth / 2, ActualHeight / 2);
                card.RenderTransform = sc;
                var ease = new BackEase { Amplitude = 0.5, EasingMode = EasingMode.EaseOut };
                sc.BeginAnimation(ScaleTransform.ScaleXProperty, new DoubleAnimation(1, TimeSpan.FromMilliseconds(260)) { EasingFunction = ease });
                sc.BeginAnimation(ScaleTransform.ScaleYProperty, new DoubleAnimation(1, TimeSpan.FromMilliseconds(260)) { EasingFunction = ease });
            };
            _timer.Interval = TimeSpan.FromMilliseconds(ms);
            _timer.Tick += (s, e) => { _timer.Stop(); FadeOut(); };
            _timer.Start();
        }

        private bool _closing;

        private void FadeOut()
        {
            if (_closing) return;
            _closing = true;
            _timer.Stop();
            var a = new DoubleAnimation(0, TimeSpan.FromMilliseconds(260));
            a.Completed += (s, e) => Close();
            BeginAnimation(OpacityProperty, a);
        }

        public static void Show(string main, string sub, bool corner, int ms, Action onClick = null)
        {
            try { _current?.Close(); } catch (Exception) { }
            _current = new PopupCard(main, sub, corner, ms, onClick);
            _current.Show();
        }
    }
}

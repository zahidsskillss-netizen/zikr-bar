using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.Primitives;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Effects;
using ZikrYad.Core;

namespace ZikrYad.Views
{
    /// <summary>Small building blocks for the app windows.</summary>
    public static class Ui
    {
        public static TextBlock Title(string s, double size, Color color)
        {
            return new TextBlock { Text = s, FontFamily = Theme.Titr, FontWeight = FontWeights.Bold, FontSize = size, Foreground = Theme.B(color), TextWrapping = TextWrapping.Wrap };
        }

        public static TextBlock Text(string s, double size, Color color)
        {
            return new TextBlock { Text = s, FontFamily = Theme.Body, FontSize = size, Foreground = Theme.B(color), TextWrapping = TextWrapping.Wrap, LineHeight = size * 1.5 };
        }

        public static TextBlock Strong(string s, double size, Color color)
        {
            return new TextBlock { Text = s, FontFamily = Theme.Strong, FontSize = size, Foreground = Theme.B(color), TextWrapping = TextWrapping.Wrap };
        }

        public static TextBlock Arabic(string s, double size, Color color)
        {
            return new TextBlock
            {
                Text = s, FontFamily = Theme.Amiri, FontWeight = FontWeights.Bold, FontSize = size, Foreground = Theme.B(color),
                TextWrapping = TextWrapping.Wrap, TextAlignment = TextAlignment.Center, FlowDirection = FlowDirection.RightToLeft
            };
        }

        public static Border Badge(string glyph, double size)
        {
            return new Border
            {
                Width = size, Height = size, CornerRadius = new CornerRadius(size * 0.3), Background = Theme.GreenGradient(),
                BorderBrush = Theme.B(Theme.Gold2), BorderThickness = new Thickness(1.5),
                Child = new TextBlock
                {
                    Text = glyph, FontFamily = Theme.Amiri, FontWeight = FontWeights.Bold, FontSize = size * 0.42, Foreground = Theme.B(Theme.Gold),
                    HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center
                }
            };
        }

        public static FrameworkElement Photo(double size, bool clickToFacebook = true)
        {
            var el = new System.Windows.Shapes.Ellipse
            {
                Width = size, Height = size, FlowDirection = FlowDirection.LeftToRight,
                Fill = new ImageBrush(Theme.Image("developer.png")) { Stretch = Stretch.UniformToFill },
                Stroke = Theme.B(Theme.Gold), StrokeThickness = Math.Max(2, size / 30)
            };
            if (clickToFacebook)
            {
                el.Cursor = Cursors.Hand;
                el.ToolTip = L.T("photo_hint");
                el.MouseLeftButtonUp += (s, e) => Theme.Open(Theme.Facebook);
            }
            return el;
        }

        public static StackPanel Card(StackPanel parent, string glyph, string title, string sub)
        {
            var body = new StackPanel();
            var head = new DockPanel { Margin = new Thickness(0, 0, 0, 8) };
            if (glyph != null)
            {
                var bd = Badge(glyph, 40);
                DockPanel.SetDock(bd, Dock.Left);
                head.Children.Add(bd);
            }
            var tx = new StackPanel { Margin = new Thickness(glyph != null ? 12 : 0, 0, 0, 0), VerticalAlignment = VerticalAlignment.Center };
            tx.Children.Add(Title(title, 18, Theme.Ink));
            if (sub != null)
            {
                var s = Text(sub, 13, Theme.Muted);
                s.Margin = new Thickness(0, 2, 0, 0);
                tx.Children.Add(s);
            }
            head.Children.Add(tx);
            body.Children.Add(head);

            var card = new Border
            {
                Background = Brushes.White, CornerRadius = new CornerRadius(22), BorderBrush = Theme.B(Theme.Line), BorderThickness = new Thickness(1),
                Padding = new Thickness(18, 16, 18, 18), Margin = new Thickness(16, 14, 16, 0), Child = body,
                Effect = new DropShadowEffect { BlurRadius = 14, ShadowDepth = 2, Opacity = 0.08, Direction = 270 }
            };
            parent.Children.Add(card);
            return body;
        }

        public static TextBlock Label(string s)
        {
            var t = Title(s, 14, Theme.Ink);
            t.Margin = new Thickness(0, 14, 0, 6);
            return t;
        }

        public static Button Button(string s, bool filled, Action onClick)
        {
            var b = new Button
            {
                Content = new TextBlock { Text = s, FontFamily = Theme.Strong, FontSize = 15, Foreground = filled ? Brushes.White : Theme.B(Theme.Green) },
                Cursor = Cursors.Hand, Padding = new Thickness(16, 9, 16, 11), Margin = new Thickness(0, 10, 0, 0)
            };
            b.Template = ButtonTemplate(filled ? (Brush)Theme.GreenGradient() : Brushes.White, Theme.B(Theme.Green));
            b.Click += (s2, e) => onClick();
            return b;
        }

        public static Button GoldButton(string s, Action onClick)
        {
            var b = new Button
            {
                Content = new TextBlock { Text = s, FontFamily = Theme.Strong, FontSize = 16, Foreground = Theme.B(Color.FromRgb(0x2D, 0x20, 0x02)) },
                Cursor = Cursors.Hand, Padding = new Thickness(16, 10, 16, 12), Margin = new Thickness(0, 10, 0, 0)
            };
            b.Template = ButtonTemplate(Theme.GoldGradient(), Theme.B(Theme.Gold2));
            b.Click += (s2, e) => onClick();
            return b;
        }

        private static ControlTemplate ButtonTemplate(Brush bg, Brush border)
        {
            var t = new ControlTemplate(typeof(Button));
            var bd = new FrameworkElementFactory(typeof(Border));
            bd.SetValue(Border.BackgroundProperty, bg);
            bd.SetValue(Border.BorderBrushProperty, border);
            bd.SetValue(Border.BorderThicknessProperty, new Thickness(1.5));
            bd.SetValue(Border.CornerRadiusProperty, new CornerRadius(14));
            bd.SetValue(Border.PaddingProperty, new TemplateBindingExtension(Control.PaddingProperty));
            var cp = new FrameworkElementFactory(typeof(ContentPresenter));
            cp.SetValue(ContentPresenter.HorizontalAlignmentProperty, HorizontalAlignment.Center);
            cp.SetValue(ContentPresenter.VerticalAlignmentProperty, VerticalAlignment.Center);
            bd.AppendChild(cp);
            t.VisualTree = bd;
            var hover = new Trigger { Property = UIElement.IsMouseOverProperty, Value = true };
            hover.Setters.Add(new Setter(UIElement.OpacityProperty, 0.88));
            t.Triggers.Add(hover);
            return t;
        }

        public static ComboBox Combo(string[] items, bool arabic)
        {
            var c = new ComboBox
            {
                FontFamily = arabic ? Theme.Amiri : Theme.Body, FontWeight = arabic ? FontWeights.Bold : FontWeights.Normal,
                FontSize = arabic ? 17 : 15, Padding = new Thickness(10, 6, 10, 6), MinHeight = 36
            };
            foreach (var s in items) c.Items.Add(s);
            return c;
        }

        public static Border Soft(UIElement child, double pad = 12)
        {
            return new Border { Background = Theme.B(Theme.Soft), BorderBrush = Theme.B(Theme.Line), BorderThickness = new Thickness(1), CornerRadius = new CornerRadius(16), Padding = new Thickness(pad), Child = child };
        }

        public static FrameworkElement Gap(double h) => new Border { Height = h };
    }

    /// <summary>A simple on/off switch.</summary>
    public class Toggle : Border
    {
        private readonly Border _thumb;
        private bool _on;
        public event Action<bool> Changed;

        public Toggle()
        {
            Width = 50; Height = 28; CornerRadius = new CornerRadius(14); Cursor = Cursors.Hand;
            _thumb = new Border { Width = 22, Height = 22, CornerRadius = new CornerRadius(11), Background = Brushes.White, HorizontalAlignment = HorizontalAlignment.Left, Margin = new Thickness(3, 0, 0, 0), Effect = new DropShadowEffect { BlurRadius = 4, ShadowDepth = 1, Opacity = 0.3 } };
            Child = _thumb;
            FlowDirection = FlowDirection.LeftToRight;
            Paint();
            MouseLeftButtonUp += (s, e) => { SetOn(!_on); Changed?.Invoke(_on); };
        }

        public bool On => _on;

        public void SetOn(bool on)
        {
            _on = on;
            Paint();
        }

        private void Paint()
        {
            Background = _on ? (Brush)Theme.GreenGradient() : Theme.B(Color.FromRgb(0xC9, 0xCF, 0xCB));
            var anim = new ThicknessAnimation(new Thickness(_on ? 25 : 3, 0, 0, 0), TimeSpan.FromMilliseconds(140));
            _thumb.BeginAnimation(MarginProperty, anim);
        }
    }
}

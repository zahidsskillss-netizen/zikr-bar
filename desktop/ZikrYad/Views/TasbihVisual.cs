using System;
using System.Globalization;
using System.Windows;
using System.Windows.Media;
using ZikrYad.Core;

namespace ZikrYad.Views
{
    /// <summary>Draws the tasbih counter in one of six designs (same designs as the Android app).</summary>
    public class TasbihVisual : FrameworkElement
    {
        public const int Count = 6;
        public static string Name(int i) => L.T("d" + i);

        private const double Pad = 12;
        private int _style;
        private string _text = "";
        private int _count;
        private int _target = 33;
        private double _d = 1.0;

        public void SetState(int style, string text, int count, int target)
        {
            bool relayout = style != _style || text != _text;
            _style = style; _text = text ?? ""; _count = count; _target = target;
            if (relayout) InvalidateMeasure();
            InvalidateVisual();
        }

        public void SetSize(double scale)
        {
            if (Math.Abs(scale - _d) < 0.0001) return;
            _d = scale;
            InvalidateMeasure();
            InvalidateVisual();
        }

        public double Scale => _d;

        private double Dp(double v) => v * _d;
        private int Goal => _target > 0 ? _target : 33;
        private int InRound => _count == 0 ? 0 : ((_count - 1) % Goal) + 1;
        private double Ppd => VisualTreeHelper.GetDpi(this).PixelsPerDip;

        // ------------------------------------------------ text
        private FormattedText Ft(string s, Typeface face, double size, Brush brush, double maxW = 0, bool rtl = true)
        {
            var ft = new FormattedText(s ?? "", CultureInfo.CurrentUICulture, rtl ? FlowDirection.RightToLeft : FlowDirection.LeftToRight,
                face, Math.Max(1, size), brush, Ppd);
            if (maxW > 0)
            {
                ft.MaxTextWidth = maxW;
                ft.MaxLineCount = 1;
                ft.Trimming = TextTrimming.CharacterEllipsis;
            }
            return ft;
        }

        private static void DrawCentered(DrawingContext dc, FormattedText ft, double cx, double cy)
        {
            double w = ft.MaxTextWidth > 0 ? Math.Min(ft.Width, ft.MaxTextWidth) : ft.Width;
            if (ft.FlowDirection == FlowDirection.RightToLeft)
                dc.DrawText(ft, new Point(cx + w / 2, cy - ft.Height / 2));
            else
                dc.DrawText(ft, new Point(cx - w / 2, cy - ft.Height / 2));
        }

        private double PillH(double size) => Dp(size) * 2.0;

        private double PillW(double size, double maxW)
        {
            var ft = Ft(_text, Theme.AmiriBold, Dp(size), Brushes.White);
            return Math.Min(ft.Width, maxW) + Dp(28);
        }

        private void Pill(DrawingContext dc, double cx, double top, double size, double maxW, Brush bg, Brush fg)
        {
            var ft = Ft(_text, Theme.AmiriBold, Dp(size), fg, maxW);
            double w = Math.Min(ft.Width, maxW) + Dp(28);
            double h = PillH(size);
            var r = new Rect(cx - w / 2, top, w, h);
            dc.DrawRoundedRectangle(new SolidColorBrush(Color.FromArgb(50, 0, 0, 0)), null, new Rect(r.X, r.Y + Dp(2), r.Width, r.Height), h / 2, h / 2);
            dc.DrawRoundedRectangle(bg, null, r, h / 2, h / 2);
            DrawCentered(dc, ft, cx, top + h / 2);
        }

        private string Fa(int n) => L.Num(n);

        // ------------------------------------------------ size
        protected override Size MeasureOverride(Size availableSize)
        {
            double maxPill = Dp(220), w, h;
            switch (_style)
            {
                case 0: w = Math.Max(Dp(132), PillW(15, maxPill)); h = PillH(15) + Dp(8) + Dp(122); break;
                case 1: w = Math.Max(Dp(100), PillW(15, maxPill)); h = PillH(15) + Dp(8) + Dp(96); break;
                case 2: w = Math.Max(Dp(154), PillW(16, maxPill)); h = PillH(16) + Dp(10) + Dp(150); break;
                case 3: w = Dp(280); h = Dp(60); break;
                case 4: w = Dp(200); h = Dp(196); break;
                default:
                    var ft = Ft(_text, Theme.AmiriBold, Dp(14), Brushes.White);
                    w = Math.Min(ft.Width, Dp(180)) + Dp(20) + Dp(8) + Dp(60);
                    h = Dp(60);
                    break;
            }
            return new Size(Math.Ceiling(w + Pad * 2), Math.Ceiling(h + Pad * 2));
        }

        protected override void OnRender(DrawingContext dc)
        {
            dc.PushTransform(new TranslateTransform(Pad, Pad));
            double w = ActualWidth - Pad * 2;
            switch (_style)
            {
                case 0: Ring(dc, w); break;
                case 1: Orb(dc, w); break;
                case 2: Beads(dc, w); break;
                case 3: Bar(dc, w); break;
                case 4: GoldCard(dc, w); break;
                default: Head(dc, w); break;
            }
            dc.Pop();
        }

        private static SolidColorBrush C(uint argb) =>
            new SolidColorBrush(Color.FromArgb((byte)(argb >> 24), (byte)(argb >> 16), (byte)(argb >> 8), (byte)argb));

        private void Shadow(DrawingContext dc, Geometry g, double dy)
        {
            dc.PushTransform(new TranslateTransform(0, Dp(dy)));
            dc.DrawGeometry(C(0x40000000), null, g);
            dc.Pop();
        }

        // 1 — ring counter
        private void Ring(DrawingContext dc, double w)
        {
            double cx = w / 2;
            Pill(dc, cx, 0, 15, Dp(220), C(0xB0000000), Brushes.White);
            double top = PillH(15) + Dp(8);
            double bw = Dp(118), bh = Dp(122);
            var r = new Rect(cx - bw / 2, top, bw, bh);
            var body = new RectangleGeometry(r, Dp(40), Dp(40));
            Shadow(dc, body, 4);
            var grad = new LinearGradientBrush(Color.FromRgb(0xE2, 0x95, 0x5A), Color.FromRgb(0xC9, 0x76, 0x3C), 90);
            dc.DrawGeometry(grad, new Pen(Brushes.White, Dp(4)), body);

            var lcd = new Rect(r.Left + Dp(10), top + Dp(14), bw - Dp(20), Dp(42));
            dc.DrawRoundedRectangle(C(0xFF1C1C1C), null, lcd, Dp(10), Dp(10));
            var inner = new Rect(lcd.Left + Dp(5), lcd.Top + Dp(5), lcd.Width - Dp(10), lcd.Height - Dp(10));
            dc.DrawRoundedRectangle(new LinearGradientBrush(Color.FromRgb(0xB9, 0xC2, 0xB5), Color.FromRgb(0xA3, 0xAD, 0xA0), 90), null, inner, Dp(5), Dp(5));
            var num = Ft(_count.ToString(CultureInfo.InvariantCulture), Theme.Consolas, Dp(_count >= 10000 ? 17 : 23), C(0xFF2B322A), 0, false);
            dc.DrawText(num, new Point(inner.Right - Dp(6) - num.Width, inner.Top + (inner.Height - num.Height) / 2));

            double oy = lcd.Bottom + Dp(14) + Dp(17);
            dc.DrawEllipse(C(0xFFA9662F), null, new Point(cx, oy), Dp(32), Dp(20));
            var chrome = new RadialGradientBrush(new GradientStopCollection
            {
                new GradientStop(Colors.White, 0), new GradientStop(Color.FromRgb(0xB8, 0xBE, 0xC4), 0.45), new GradientStop(Color.FromRgb(0x6F, 0x76, 0x7C), 1)
            }) { GradientOrigin = new Point(0.4, 0.3), Center = new Point(0.4, 0.3), RadiusX = 0.7, RadiusY = 0.7 };
            dc.DrawEllipse(chrome, null, new Point(cx, oy), Dp(29), Dp(17));
        }

        private static Geometry Arc(Point c, double r, double startDeg, double sweepDeg)
        {
            if (sweepDeg >= 359.99) return new EllipseGeometry(c, r, r);
            double a0 = startDeg * Math.PI / 180, a1 = (startDeg + sweepDeg) * Math.PI / 180;
            var p0 = new Point(c.X + r * Math.Cos(a0), c.Y + r * Math.Sin(a0));
            var p1 = new Point(c.X + r * Math.Cos(a1), c.Y + r * Math.Sin(a1));
            var fig = new PathFigure { StartPoint = p0, IsClosed = false };
            fig.Segments.Add(new ArcSegment(p1, new Size(r, r), 0, sweepDeg > 180, SweepDirection.Clockwise, true));
            var g = new PathGeometry();
            g.Figures.Add(fig);
            return g;
        }

        // 2 — green orb with a gold progress ring
        private void Orb(DrawingContext dc, double w)
        {
            double cx = w / 2;
            Pill(dc, cx, 0, 15, Dp(220), C(0xFF1B5E20), Brushes.White);
            double cy = PillH(15) + Dp(8) + Dp(48);
            var c = new Point(cx, cy);
            double ir = Dp(39);
            dc.DrawEllipse(C(0x40000000), null, new Point(cx, cy + Dp(4)), ir, ir);
            var g = new RadialGradientBrush(Color.FromRgb(0x43, 0xA0, 0x47), Color.FromRgb(0x1B, 0x5E, 0x20))
            { GradientOrigin = new Point(0.35, 0.3), Center = new Point(0.35, 0.3), RadiusX = 0.8, RadiusY = 0.8 };
            dc.DrawEllipse(g, null, c, ir, ir);
            dc.DrawEllipse(null, new Pen(C(0x88FFFFFF), Dp(6)), c, Dp(44), Dp(44));
            double sweep = 360.0 * InRound / Goal;
            if (sweep > 0)
            {
                var pen = new Pen(C(0xFFC9A227), Dp(6)) { StartLineCap = PenLineCap.Round, EndLineCap = PenLineCap.Round };
                dc.DrawGeometry(null, pen, Arc(c, Dp(44), -90, sweep));
            }
            var n = Ft(Fa(_count), Theme.SegoeBold, Dp(_count >= 1000 ? 21 : 28), Brushes.White);
            if (_target > 0)
            {
                DrawCentered(dc, n, cx, cy - Dp(6));
                DrawCentered(dc, Ft("/ " + Fa(_target), Theme.SegoeBold, Dp(11), C(0xCCFFFFFF)), cx, cy + Dp(17));
            }
            else DrawCentered(dc, n, cx, cy);
        }

        // 3 — 33 beads that light up
        private void Beads(DrawingContext dc, double w)
        {
            double cx = w / 2;
            Pill(dc, cx, 0, 16, Dp(220), C(0xFFFFF8E8), C(0xFF3E2A12));
            double cy = PillH(16) + Dp(10) + Dp(75);
            dc.DrawEllipse(C(0x40000000), null, new Point(cx, cy + Dp(4)), Dp(75), Dp(75));
            dc.DrawEllipse(C(0xFF3E2A12), null, new Point(cx, cy), Dp(75), Dp(75));
            int on = (int)Math.Round(InRound * 33.0 / Goal);
            var lit = new RadialGradientBrush(Color.FromRgb(0xFF, 0xF4, 0xC2), Color.FromRgb(0xD4, 0xA7, 0x2C))
            { GradientOrigin = new Point(0.35, 0.3), Center = new Point(0.35, 0.3) };
            var dim = C(0xFF6B4B25);
            for (int i = 0; i < 33; i++)
            {
                double a = i / 33.0 * Math.PI * 2 - Math.PI / 2;
                dc.DrawEllipse(i < on ? (Brush)lit : dim, null, new Point(cx + Dp(62) * Math.Cos(a), cy + Dp(62) * Math.Sin(a)), Dp(5), Dp(5));
            }
            DrawCentered(dc, Ft(Fa(_count), Theme.SegoeBold, Dp(_count >= 1000 ? 26 : 34), C(0xFFFFF1CF)), cx, cy);
        }

        // 4 — dark bar
        private void Bar(DrawingContext dc, double w)
        {
            var r = new Rect(0, 0, w, Dp(60));
            dc.DrawRoundedRectangle(C(0x40000000), null, new Rect(0, Dp(4), w, Dp(60)), Dp(30), Dp(30));
            dc.DrawRoundedRectangle(C(0xE6101611), null, r, Dp(30), Dp(30));
            double pcx = w - Dp(8) - Dp(22);
            dc.DrawEllipse(C(0xFF2E7D32), null, new Point(pcx, Dp(30)), Dp(22), Dp(22));
            DrawCentered(dc, Ft("+", Theme.SegoeBold, Dp(28), Brushes.White, 0, false), pcx, Dp(29));
            var n = Ft(Fa(_count), Theme.SegoeBold, Dp(20), C(0xFF9BE29F));
            double nRight = w - Dp(8) - Dp(44) - Dp(10);
            dc.DrawText(n, new Point(nRight, Dp(30) - n.Height / 2));
            double textRight = nRight - n.Width - Dp(12);
            var t = Ft(_text, Theme.AmiriBold, Dp(17), Brushes.White, Math.Max(10, textRight - Dp(18)));
            dc.DrawText(t, new Point(textRight, Dp(30) - t.Height / 2));
        }

        // 5 — gold card
        private void GoldCard(DrawingContext dc, double w)
        {
            double h = Dp(196);
            dc.DrawRoundedRectangle(C(0x50000000), null, new Rect(0, Dp(5), w, h), Dp(22), Dp(22));
            var bg = new LinearGradientBrush(Color.FromRgb(0x0F, 0x3D, 0x1F), Color.FromRgb(0x07, 0x26, 0x13), 45);
            dc.DrawRoundedRectangle(bg, new Pen(C(0xFFC9A227), Dp(1.5)), new Rect(Dp(0.75), Dp(0.75), w - Dp(1.5), h - Dp(1.5)), Dp(22), Dp(22));
            double cx = w / 2;
            DrawCentered(dc, Ft(_text, Theme.AmiriBold, Dp(20), C(0xFFF3E3A5), w - Dp(28)), cx, Dp(34));
            DrawCentered(dc, Ft(Fa(_count), Theme.SegoeBold, Dp(_count >= 10000 ? 26 : 34), Brushes.White), cx, Dp(80));
            double by = Dp(142);
            dc.DrawEllipse(C(0xFF7A5B08), null, new Point(cx, by + Dp(4)), Dp(32), Dp(32));
            var gold = new RadialGradientBrush(Color.FromRgb(0xF6, 0xDD, 0x7D), Color.FromRgb(0xB8, 0x8A, 0x12))
            { GradientOrigin = new Point(0.35, 0.3), Center = new Point(0.35, 0.3), RadiusX = 0.8, RadiusY = 0.8 };
            dc.DrawEllipse(gold, null, new Point(cx, by), Dp(32), Dp(32));
            DrawCentered(dc, Ft("ﷲ", Theme.AmiriBold, Dp(26), C(0xFF2D2002)), cx, by);
        }

        // 6 — small head with a dhikr bubble
        private void Head(DrawingContext dc, double w)
        {
            double cx = w - Dp(30), cy = Dp(30);
            dc.DrawEllipse(C(0x40000000), null, new Point(cx, cy + Dp(3)), Dp(30), Dp(30));
            dc.DrawEllipse(Brushes.White, null, new Point(cx, cy), Dp(30), Dp(30));
            dc.DrawEllipse(C(0xFF1B5E20), null, new Point(cx, cy), Dp(27), Dp(27));
            DrawCentered(dc, Ft(Fa(_count), Theme.SegoeBold, Dp(_count >= 1000 ? 15 : 20), Brushes.White), cx, cy);
            var t = Ft(_text, Theme.AmiriBold, Dp(14), Brushes.White, Dp(180));
            double bw = Math.Min(t.Width, Dp(180)) + Dp(20);
            double right = w - Dp(60) - Dp(8);
            var r = new Rect(right - bw, Dp(14), bw, Dp(32));
            dc.DrawRoundedRectangle(C(0xE61B5E20), null, r, Dp(10), Dp(10));
            DrawCentered(dc, t, r.Left + r.Width / 2, r.Top + r.Height / 2);
        }
    }
}

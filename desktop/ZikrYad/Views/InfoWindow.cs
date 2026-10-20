using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Effects;
using ZikrYad.Core;

namespace ZikrYad.Views
{
    /// <summary>About the app, about the developer, and support (donate) pages.</summary>
    public class InfoWindow : Window
    {
        public const int PageAbout = 0, PageDev = 1, PageDonate = 2;

        public InfoWindow(int page)
        {
            Width = 540;
            Height = 820;
            MinWidth = 420;
            WindowStartupLocation = WindowStartupLocation.CenterOwner;
            Background = Theme.B(Theme.Bg);
            Icon = Theme.Image("app.ico");
            FlowDirection = L.Rtl ? FlowDirection.RightToLeft : FlowDirection.LeftToRight;
            UseLayoutRounding = true;

            var root = new StackPanel { Margin = new Thickness(0, 0, 0, 32) };
            if (page == PageDev) Developer(root);
            else if (page == PageDonate) Donate(root);
            else About(root);
            Content = new ScrollViewer { Content = root, VerticalScrollBarVisibility = ScrollBarVisibility.Auto };
        }

        // ------------------------------------------------ pages
        private void About(StackPanel root)
        {
            Title = L.T("nav_about");
            var logo = new Image { Source = Theme.Image("logo.png"), Width = 88, Height = 88, FlowDirection = FlowDirection.LeftToRight };
            root.Children.Add(Header(logo, L.T("nav_about"), L.T("app_name") + " · " + L.T("version") + " 1.0 (Windows)"));

            var goal = Ui.Card(root, "✎", L.T("goal_t"), null);
            goal.Children.Add(Para(L.T("goal_text")));
            var dua = Ui.Arabic("جَزَاكَ اللَّهُ خَيْرًا كَثِيرًا", 21, Theme.Green);
            dua.Margin = new Thickness(0, 12, 0, 4);
            goal.Children.Add(dua);
            goal.Children.Add(Signature());

            var feat = Ui.Card(root, "✦", L.T("feat_t"), null);
            string[] marks = { "ﷲ", L.Num(1), L.Num(2), L.Num(3), L.Num(4), L.Num(5) };
            for (int i = 0; i < 6; i++) feat.Children.Add(Bullet(marks[i], L.T("f" + (i + 1))));

            var privacy = Ui.Card(root, "✓", L.T("privacy_t"), null);
            privacy.Children.Add(Para(L.T("privacy")));

            var share = Ui.Card(root, "↗", L.T("share_btn"), null);
            share.Children.Add(Para(L.T("share_text")));
            share.Children.Add(Ui.Button(L.T("share_now"), true, () => Share(this)));
        }

        private void Developer(StackPanel root)
        {
            Title = L.T("nav_dev");
            root.Children.Add(Header(Ui.Photo(120), L.T("dev_name"), L.Lang == L.EN ? L.T("ghora") : "Zahid Ullah Alokozay"));

            var me = Ui.Card(root, "✎", L.T("about_me"), null);
            me.Children.Add(Para(L.T("me_text")));
            var chips = new WrapPanel { Margin = new Thickness(0, 10, 0, 0) };
            foreach (var k in new[] { "chip1", "chip2", "chip3" }) chips.Children.Add(Chip(L.T(k)));
            me.Children.Add(chips);
            var hint = Ui.Text(L.T("photo_hint"), 13, Theme.Muted);
            hint.Margin = new Thickness(0, 10, 0, 0);
            me.Children.Add(hint);

            var contact = Ui.Card(root, "☎", L.T("contact"), null);
            contact.Children.Add(ContactRow(L.T("whatsapp"), Theme.Phone, Theme.WhatsApp));
            contact.Children.Add(ContactRow(L.T("telegram"), "pashtoapp", Theme.Channel));
            contact.Children.Add(ContactRow(L.T("facebook"), "zahid.alkozai", Theme.Facebook));

            root.Children.Add(GhoraAd());

            var support = Ui.Card(root, "♥", L.T("support_t"), null);
            support.Children.Add(Para(L.T("support_text")));
            support.Children.Add(Ui.Button(L.T("donate_btn"), true, () => new InfoWindow(PageDonate) { Owner = Owner ?? this }.Show()));
        }

        private UIElement GhoraAd()
        {
            var ad = new StackPanel();
            var brand = Ui.Title(L.T("ghora"), 27, Theme.Gold);
            brand.TextAlignment = TextAlignment.Center;
            ad.Children.Add(brand);
            if (L.Lang != L.EN)
            {
                var en = Ui.Strong("GHORA GRAPHICS PRODUCTION", 11, Color.FromArgb(0xB3, 0xF3, 0xE3, 0xA5));
                en.TextAlignment = TextAlignment.Center;
                en.FlowDirection = FlowDirection.LeftToRight;
                en.Margin = new Thickness(0, 4, 0, 0);
                ad.Children.Add(en);
            }
            var tag = Ui.Text(L.T("ad_tag"), 15, Colors.White);
            tag.TextAlignment = TextAlignment.Center;
            tag.Margin = new Thickness(0, 10, 0, 10);
            ad.Children.Add(tag);
            foreach (var k in new[] { "s1", "chip2", "s3", "s4", "s5" })
            {
                var row = new DockPanel { Margin = new Thickness(0, 4, 0, 4) };
                var dot = Ui.Text("✦", 13, Theme.Gold);
                DockPanel.SetDock(dot, Dock.Left);
                row.Children.Add(dot);
                var t = Ui.Text(L.T(k), 15.5, Colors.White);
                t.Margin = new Thickness(10, 0, 10, 0);
                row.Children.Add(t);
                ad.Children.Add(row);
            }
            var call = Ui.Title(L.T("contact_now"), 15, Theme.Gold);
            call.TextAlignment = TextAlignment.Center;
            call.Margin = new Thickness(0, 14, 0, 0);
            ad.Children.Add(call);
            ad.Children.Add(Ui.GoldButton(L.T("whatsapp") + ": " + Theme.Phone, () => Theme.Open(Theme.WhatsApp)));
            ad.Children.Add(Ui.Button(L.T("telegram") + " · pashtoapp", false, () => Theme.Open(Theme.Channel)));
            return new Border
            {
                Background = new LinearGradientBrush(Color.FromRgb(0x14, 0x43, 0x2A), Color.FromRgb(0x06, 0x1A, 0x0E), 45),
                BorderBrush = Theme.B(Theme.Gold2), BorderThickness = new Thickness(1.5), CornerRadius = new CornerRadius(24),
                Padding = new Thickness(20, 22, 20, 20), Margin = new Thickness(16, 14, 16, 0), Child = ad,
                Effect = new DropShadowEffect { BlurRadius = 18, ShadowDepth = 4, Opacity = 0.2, Direction = 270 }
            };
        }

        private void Donate(StackPanel root)
        {
            Title = L.T("support_t");
            root.Children.Add(Header(Ui.Badge("♥", 86), L.T("support_t"), L.T("donate_sub")));

            var intro = Ui.Card(root, null, L.T("why_t"), null);
            var who = new DockPanel { Margin = new Thickness(0, 4, 0, 10) };
            var ph = Ui.Photo(52);
            DockPanel.SetDock(ph, Dock.Left);
            who.Children.Add(ph);
            var wt = new StackPanel { Margin = new Thickness(12, 0, 12, 0), VerticalAlignment = VerticalAlignment.Center };
            wt.Children.Add(Ui.Title(L.T("dev_name"), 16, Theme.Ink));
            wt.Children.Add(Ui.Text(L.T("maker") + " · " + L.T("ghora"), 12.5, Theme.Muted));
            who.Children.Add(wt);
            intro.Children.Add(who);
            intro.Children.Add(Para(L.T("why_text")));

            PayCard(root, "H", "HesabPay", Theme.Hesab);
            PayCard(root, "A", "Atoma Pay", Theme.Atoma);

            var pay = Ui.Card(root, "P", "Payoneer", L.T("payoneer_s"));
            pay.Children.Add(ValueBox(Theme.Payoneer));
            pay.Children.Add(Ui.Button(L.T("copy_email"), false, () => Copy(Theme.Payoneer, L.T("copied"))));

            var dua = Ui.Arabic("جَزَاكُمُ اللَّهُ خَيْرًا", 24, Theme.Green);
            dua.Margin = new Thickness(0, 22, 0, 0);
            root.Children.Add(dua);
        }

        /// <summary>Shows the full number; Copy gives the 9 digits without the leading 0.</summary>
        private void PayCard(StackPanel root, string glyph, string title, string number)
        {
            var c = Ui.Card(root, glyph, title, L.T("mobile_no"));
            c.Children.Add(ValueBox(number));
            string copyValue = number.StartsWith("0") ? number.Substring(1) : number;
            c.Children.Add(Ui.Button(L.T("copy") + "  (" + copyValue + ")", false, () => Copy(copyValue, L.F("copied_9", copyValue))));
        }

        // ------------------------------------------------ actions
        public static void Share(Window owner)
        {
            string msg = L.T("share_msg") + "\n" + Theme.ShareLink + "\n\nفَاذْكُرُونِي أَذْكُرْكُمْ";
            try { Clipboard.SetText(msg); } catch (Exception) { }
            PopupCard.Show(L.T("copied"), L.T("share_btn"), true, 2200);
        }

        private static void Copy(string value, string message)
        {
            try { Clipboard.SetText(value); } catch (Exception) { }
            PopupCard.Show(L.T("copied"), message, true, 3500);
        }

        // ------------------------------------------------ pieces
        private UIElement Header(FrameworkElement top, string title, string sub)
        {
            var h = new StackPanel();
            var back = Ui.Strong((L.Rtl ? "→  " : "←  ") + L.T("back"), 14, Color.FromArgb(0xE6, 0xFF, 0xFF, 0xFF));
            var bb = new Border
            {
                Child = back, Background = Theme.B(0x26, Colors.White), CornerRadius = new CornerRadius(16), Padding = new Thickness(14, 5, 16, 7),
                HorizontalAlignment = HorizontalAlignment.Left, Cursor = Cursors.Hand, Margin = new Thickness(0, 0, 0, 12)
            };
            bb.MouseLeftButtonUp += (s, e) => Close();
            h.Children.Add(bb);
            top.HorizontalAlignment = HorizontalAlignment.Center;
            h.Children.Add(top);
            var t = Ui.Title(title, 28, Colors.White);
            t.TextAlignment = TextAlignment.Center;
            t.Margin = new Thickness(0, 12, 0, 4);
            h.Children.Add(t);
            var s2 = Ui.Text(sub, 13.5, Color.FromArgb(0xD9, 0xFF, 0xFF, 0xFF));
            s2.TextAlignment = TextAlignment.Center;
            h.Children.Add(s2);
            return new Border { Background = Theme.GreenGradient(), CornerRadius = new CornerRadius(0, 0, 32, 32), Padding = new Thickness(18, 14, 18, 26), Child = h };
        }

        private UIElement Signature()
        {
            var row = new DockPanel { Margin = new Thickness(0, 10, 0, 0) };
            var ph = Ui.Photo(42);
            DockPanel.SetDock(ph, Dock.Left);
            row.Children.Add(ph);
            var st = new StackPanel { Margin = new Thickness(10, 0, 10, 0), VerticalAlignment = VerticalAlignment.Center };
            st.Children.Add(Ui.Title(L.T("dev_name"), 15, Theme.Ink));
            st.Children.Add(Ui.Text(L.T("maker"), 12.5, Theme.Muted));
            row.Children.Add(st);
            return row;
        }

        private static TextBlock Para(string s)
        {
            var t = Ui.Text(s, 16, Theme.Ink);
            t.LineHeight = 26;
            return t;
        }

        private static UIElement Chip(string s)
        {
            return new Border
            {
                Background = Theme.B(0x14, Theme.Green), CornerRadius = new CornerRadius(14), Padding = new Thickness(12, 3, 12, 5), Margin = new Thickness(0, 0, 6, 6),
                Child = Ui.Strong(s, 12.5, Theme.Green)
            };
        }

        private static UIElement Bullet(string mark, string s)
        {
            var row = new DockPanel { Margin = new Thickness(0, 6, 0, 6) };
            var b = Ui.Badge(mark, 32);
            DockPanel.SetDock(b, Dock.Left);
            row.Children.Add(b);
            var t = Ui.Text(s, 15, Theme.Ink);
            t.Margin = new Thickness(12, 0, 12, 0);
            t.VerticalAlignment = VerticalAlignment.Center;
            row.Children.Add(t);
            return row;
        }

        private static UIElement ContactRow(string label, string value, string url)
        {
            var row = new DockPanel();
            var l = Ui.Title(label, 15, Theme.Ink);
            l.Width = 90;
            DockPanel.SetDock(l, Dock.Left);
            row.Children.Add(l);
            var v = Ui.Text(value, 15, Theme.Green);
            v.FlowDirection = FlowDirection.LeftToRight;
            v.HorizontalAlignment = HorizontalAlignment.Right;
            row.Children.Add(v);
            var b = Ui.Soft(row, 12);
            b.Margin = new Thickness(0, 8, 0, 0);
            b.Cursor = Cursors.Hand;
            b.MouseLeftButtonUp += (s, e) => Theme.Open(url);
            return b;
        }

        private static UIElement ValueBox(string v)
        {
            var t = Ui.Title(v, 22, Theme.Green);
            t.TextAlignment = TextAlignment.Center;
            t.FlowDirection = FlowDirection.LeftToRight;
            var b = Ui.Soft(t, 14);
            b.Margin = new Thickness(0, 8, 0, 0);
            return b;
        }
    }
}

using System;
using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Imaging;

namespace ZikrYad.Core
{
    /// <summary>Shared colors, brushes, fonts and images.</summary>
    public static class Theme
    {
        public static readonly Color Green = Color.FromRgb(0x1B, 0x5E, 0x20);
        public static readonly Color Green2 = Color.FromRgb(0x2E, 0x7D, 0x32);
        public static readonly Color Dark = Color.FromRgb(0x0B, 0x33, 0x19);
        public static readonly Color Ink = Color.FromRgb(0x16, 0x23, 0x1A);
        public static readonly Color Muted = Color.FromRgb(0x5F, 0x6E, 0x64);
        public static readonly Color Line = Color.FromRgb(0xE1, 0xE8, 0xE0);
        public static readonly Color Bg = Color.FromRgb(0xF1, 0xF4, 0xEF);
        public static readonly Color Soft = Color.FromRgb(0xF6, 0xF8, 0xF4);
        public static readonly Color Gold = Color.FromRgb(0xF3, 0xE3, 0xA5);
        public static readonly Color Gold2 = Color.FromRgb(0xC9, 0xA2, 0x27);

        public static SolidColorBrush B(Color c) { var b = new SolidColorBrush(c); b.Freeze(); return b; }
        public static SolidColorBrush B(byte a, Color c) { var b = new SolidColorBrush(Color.FromArgb(a, c.R, c.G, c.B)); b.Freeze(); return b; }

        public static LinearGradientBrush GreenGradient()
        {
            var g = new LinearGradientBrush(Green2, Dark, new Point(0, 0), new Point(1, 1));
            g.Freeze();
            return g;
        }

        public static LinearGradientBrush GoldGradient()
        {
            var g = new LinearGradientBrush(Color.FromRgb(0xF6, 0xDD, 0x7D), Gold2, 90);
            g.Freeze();
            return g;
        }

        private static readonly Uri Base = new Uri("pack://application:,,,/");

        public static readonly FontFamily Titr = new FontFamily(Base, "./Assets/Fonts/bahij_titr.ttf#Bahij Titr, Segoe UI");
        public static readonly FontFamily Body = new FontFamily(Base,
            "./Assets/Fonts/bahij_light.ttf#Bahij TheSansArabic SemiLight, ./Assets/Fonts/bahij_light.ttf#Bahij TheSansArabic, Segoe UI");
        public static readonly FontFamily Strong = new FontFamily(Base,
            "./Assets/Fonts/bahij_body.ttf#Bahij TheSansArabic Black, ./Assets/Fonts/bahij_body.ttf#Bahij TheSansArabic, Segoe UI");
        public static readonly FontFamily Amiri = new FontFamily(Base, "./Assets/Fonts/#Amiri, Traditional Arabic");

        public static readonly Typeface AmiriBold = new Typeface(Amiri, FontStyles.Normal, FontWeights.Bold, FontStretches.Normal);
        public static readonly Typeface StrongFace = new Typeface(Strong, FontStyles.Normal, FontWeights.Normal, FontStretches.Normal);
        public static readonly Typeface TitrFace = new Typeface(Titr, FontStyles.Normal, FontWeights.Bold, FontStretches.Normal);
        public static readonly Typeface SegoeBold = new Typeface(new FontFamily("Segoe UI"), FontStyles.Normal, FontWeights.Bold, FontStretches.Normal);
        public static readonly Typeface Consolas = new Typeface(new FontFamily("Consolas"), FontStyles.Normal, FontWeights.Bold, FontStretches.Normal);

        public static BitmapImage Image(string name)
        {
            var bmp = new BitmapImage();
            bmp.BeginInit();
            bmp.UriSource = new Uri("pack://application:,,,/Assets/" + name);
            bmp.CacheOption = BitmapCacheOption.OnLoad;
            bmp.EndInit();
            bmp.Freeze();
            return bmp;
        }

        public const string Facebook = "https://www.facebook.com/zahid.alkozai";
        public const string Telegram = "https://t.me/zahid_af";
        public const string Channel = "https://www.telegram.me/pashtoapp";
        public const string Phone = "0780630100";
        public const string WhatsApp = "https://wa.me/93780630100";
        public const string Hesab = "0780630100";
        public const string Atoma = "0773233182";
        public const string Payoneer = "zahidalkozii@gmail.com";
        public const string ShareLink = "https://github.com/zahidsskillss-netizen/zikr-bar/releases/latest";

        public static void Open(string url)
        {
            try
            {
                System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(url) { UseShellExecute = true });
            }
            catch (Exception) { }
        }
    }
}

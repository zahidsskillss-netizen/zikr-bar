using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;

namespace ZikrYad.Core
{
    /// <summary>Everything the app remembers, saved as JSON in %AppData%\ZikrYad.</summary>
    public class Settings
    {
        public const string DefaultList =
            "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ عَلَى نَبِيِّنَا مُحَمَّد\n"
            + "فَاذْكُرُونِي أَذْكُرْكُمْ\n"
            + "سُبْحَانَ اللَّهِ\n"
            + "الْحَمْدُ لِلَّهِ\n"
            + "لَا إِلَهَ إِلَّا اللَّهُ\n"
            + "اللَّهُ أَكْبَرُ\n"
            + "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ\n"
            + "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ\n"
            + "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ\n"
            + "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ\n"
            + "لَا إِلَهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ\n"
            + "حَسْبِيَ اللَّهُ لَا إِلَهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ\n"
            + "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ ﷺ نَبِيًّا\n"
            + "سُبْحَانَ اللَّهِ وَالْحَمْدُ لِلَّهِ وَلَا إِلَهَ إِلَّا اللَّهُ وَاللَّهُ أَكْبَرُ\n"
            + "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ";

        public static readonly int[] Intervals = { 1, 5, 15, 30, 60 };
        public static readonly int[] Targets = { 33, 99, 100, 1000, 0 };
        public static readonly int[] ReminderHoursList = { 0, 1, 2, 3 };

        public int Lang { get; set; } = 0;
        public string RawList { get; set; } = DefaultList;
        public int Index { get; set; } = 0;
        public int IntervalMinutes { get; set; } = 5;
        public bool PopupDhikr { get; set; } = true;
        public bool Overlay { get; set; } = true;
        public int TasbihStyle { get; set; } = 0;
        public int TasbihIndex { get; set; } = 2;
        public int Target { get; set; } = 33;
        public int Count { get; set; } = 0;
        public double PosX { get; set; } = double.NaN;
        public double PosY { get; set; } = double.NaN;
        public double Scale { get; set; } = 1.0;
        public int Mini { get; set; } = 0;          // 0 shown, 1 hidden left, 2 hidden right
        public double MiniY { get; set; } = 400;
        public int ReminderHours { get; set; } = 2;
        public DateTime LastZikr { get; set; } = DateTime.MinValue;
        public DateTime LastReminder { get; set; } = DateTime.MinValue;
        public bool UnlockPopup { get; set; } = true;
        public bool Autostart { get; set; } = true;
        public bool FirstRun { get; set; } = true;

        // ---------------- helpers ----------------
        public List<string> List()
        {
            var l = (RawList ?? "").Split('\n').Select(s => s.Trim()).Where(s => s.Length > 0).ToList();
            if (l.Count == 0) l.Add("سُبْحَانَ اللَّهِ");
            return l;
        }

        public string Current()
        {
            var l = List();
            return l[Mod(Index, l.Count)];
        }

        public void Next() => Index = Mod(Index + 1, List().Count);

        public int TasbihIdx() => Mod(TasbihIndex, List().Count);

        public string TasbihText() => List()[TasbihIdx()];

        public string NextUnlockText()
        {
            var l = List();
            _unlock = Mod(_unlock + 1, l.Count);
            return l[_unlock];
        }

        private int _unlock = -1;

        public void SaveList(string raw)
        {
            RawList = raw;
            Index = 0;
            TasbihIndex = 0;
        }

        private static int Mod(int a, int n) => ((a % n) + n) % n;

        // ---------------- storage ----------------
        public static string Folder => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "ZikrYad");
        private static string FilePath => Path.Combine(Folder, "settings.json");

        public static Settings Load()
        {
            try
            {
                if (File.Exists(FilePath))
                {
                    var s = JsonSerializer.Deserialize<Settings>(File.ReadAllText(FilePath));
                    if (s != null) return s;
                }
            }
            catch (Exception) { }
            return new Settings();
        }

        public void Save()
        {
            try
            {
                Directory.CreateDirectory(Folder);
                File.WriteAllText(FilePath, JsonSerializer.Serialize(this, new JsonSerializerOptions { WriteIndented = true }));
            }
            catch (Exception) { }
        }
    }
}

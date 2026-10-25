package com.ghora.zikrbar;

/** Places for prayer times: all provincial centres of Afghanistan first, then nearby and holy cities. */
final class Cities {

    static final class City {
        final String ps, en, tz;
        final double lat, lng;

        City(String ps, String en, double lat, double lng, String tz) {
            this.ps = ps;
            this.en = en;
            this.lat = lat;
            this.lng = lng;
            this.tz = tz;
        }

        String name() {
            return L.lang == L.EN ? en : ps;
        }
    }

    private Cities() {}

    private static final String AF = "Asia/Kabul", PK = "Asia/Karachi", SA = "Asia/Riyadh";

    static final City[] ALL = {
            new City("کابل", "Kabul", 34.5553, 69.2075, AF),
            new City("کندهار", "Kandahar", 31.6289, 65.7372, AF),
            new City("هرات", "Herat", 34.3529, 62.2040, AF),
            new City("مزار شریف", "Mazar-i-Sharif", 36.7090, 67.1109, AF),
            new City("جلال اباد", "Jalalabad", 34.4342, 70.4478, AF),
            new City("مهترلام", "Mehtar Lam", 34.6714, 70.2094, AF),
            new City("کندز", "Kunduz", 36.7280, 68.8680, AF),
            new City("غزني", "Ghazni", 33.5536, 68.4269, AF),
            new City("خوست", "Khost", 33.3395, 69.9204, AF),
            new City("ګردېز", "Gardez", 33.5974, 69.2259, AF),
            new City("لښکرګاه", "Lashkar Gah", 31.5938, 64.3700, AF),
            new City("بامیان", "Bamyan", 34.8210, 67.8270, AF),
            new City("اسعداباد", "Asadabad", 34.8742, 71.1528, AF),
            new City("پلخمري", "Pul-i-Khumri", 35.9446, 68.7151, AF),
            new City("فراه", "Farah", 32.3746, 62.1164, AF),
            new City("زرنج", "Zaranj", 30.9598, 61.8608, AF),
            new City("تالقان", "Taloqan", 36.7361, 69.5345, AF),
            new City("فیض اباد", "Faizabad", 37.1166, 70.5800, AF),
            new City("شبرغان", "Sheberghan", 36.6650, 65.7520, AF),
            new City("میمنه", "Maimana", 35.9213, 64.7836, AF),
            new City("چاریکار", "Charikar", 35.0136, 69.1714, AF),
            new City("پل علم", "Pul-i-Alam", 33.9953, 69.0227, AF),
            new City("میدان ښار", "Maidan Shahr", 34.3956, 68.8660, AF),
            new City("محمود راقي", "Mahmud-i-Raqi", 35.0167, 69.3333, AF),
            new City("شرنه", "Sharana", 33.1757, 68.7304, AF),
            new City("ترینکوټ", "Tarinkot", 32.6268, 65.8733, AF),
            new City("قلات", "Qalat", 32.1058, 66.9083, AF),
            new City("چغچران", "Chaghcharan", 34.5225, 65.2517, AF),
            new City("ایبک", "Aybak", 36.2647, 68.0155, AF),
            new City("سرپل", "Sar-e Pol", 36.2154, 65.9325, AF),
            new City("قلعه نو", "Qala-i-Naw", 34.9846, 63.1333, AF),
            new City("نیلي", "Nili", 33.7218, 66.1302, AF),
            new City("بازارک", "Bazarak", 35.3129, 69.5152, AF),
            new City("پارون", "Parun", 35.4206, 70.9226, AF),
            new City("پېښور", "Peshawar", 34.0151, 71.5249, PK),
            new City("کوټه", "Quetta", 30.1798, 66.9750, PK),
            new City("اسلام اباد", "Islamabad", 33.6844, 73.0479, PK),
            new City("کراچۍ", "Karachi", 24.8607, 67.0011, PK),
            new City("لاهور", "Lahore", 31.5204, 74.3587, PK),
            new City("مکه مکرمه", "Makkah", 21.3891, 39.8579, SA),
            new City("مدینه منوره", "Madinah", 24.5247, 39.5692, SA),
            new City("دوبۍ", "Dubai", 25.2048, 55.2708, "Asia/Dubai"),
            new City("استانبول", "Istanbul", 41.0082, 28.9784, "Europe/Istanbul"),
            new City("تهران", "Tehran", 35.6892, 51.3890, "Asia/Tehran"),
            new City("لندن", "London", 51.5074, -0.1278, "Europe/London"),
    };
}

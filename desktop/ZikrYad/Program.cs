using System;
using System.Linq;
using System.Threading;
using System.Windows;

namespace ZikrYad
{
    public static class Program
    {
        [STAThread]
        public static void Main(string[] args)
        {
            using (var mutex = new Mutex(true, "ZikrYad.SingleInstance.Ghora", out bool first))
            {
                if (!first)
                {
                    // Already running: ask the running copy to show its window.
                    try
                    {
                        using (var ev = EventWaitHandle.OpenExisting("ZikrYad.Show.Ghora")) ev.Set();
                    }
                    catch (Exception) { }
                    return;
                }

                var app = new Application { ShutdownMode = ShutdownMode.OnExplicitShutdown };
                bool startHidden = args.Any(a => a.Equals("--tray", StringComparison.OrdinalIgnoreCase));
                app.Startup += (s, e) => ZikrApp.Start(startHidden);
                app.Exit += (s, e) => ZikrApp.Stop();
                app.Run();
            }
        }
    }
}

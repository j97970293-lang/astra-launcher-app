# Règles R8 d'Astra Launcher.
#
# Rien n'est réfléchi ici : l'application n'utilise ni sérialisation par
# reflection, ni bibliothèque tierce, ni JNI. Les règles ne servent qu'à garder
# le point d'entrée du lanceur tel qu'il est déclaré dans le manifeste, au cas
# où une réduction future tracerait l'activité comme inatteignable.
-keep public class com.astraforge.launcher.MainActivity {
    public <init>(...);
    public void onCreate(android.os.Bundle);
    public void onResume();
}

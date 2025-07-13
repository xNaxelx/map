package noescape.downloader;

public enum CourtRegion {
    KRYM("2"),
    VINNITZA("3"),
    VOLYN("4"),
    DIPRO("5"),
    DONETZK("6"),
    ZHYTOMIR("7"),
    ZAKARPAT("8"),
    ZPORIZH("9"),
    IVANO_FRANKIVSK("10"),
    KYIVSKA("11"),
    KIROVOGRAD("12"),
    LUHANSK("13"),
    LVIV("14"),
    MYKOLAIV("15"),
    ODESA("16"),
    POLTAVA("17"),
    RIVNE("18"),
    SUMY("19"),
    TERNOPIL("20"),
    KHARKIV("21"),
    KHMELNITSK("23"),
    CHERKASY("24"),
    CHERNIVCI("25"),
    CHERNYGIV("26"),
    MISTO_KYIV("27"),
    MISTO_SEVASTOP("28"),
    VS_CENTRAL("29"),
    VS_ZAHID("30"),
    VS_PIVDEN("31"),
    VS_VMS_UKR("32"),
    ;

    public final String code;

    CourtRegion(String code) {
        this.code = code;
    }
}

package cdphandler;

public enum CdpKey {
    Backspace(8),
    Tab(9),
    Enter(13),

    Shift(16),
    Control(17),
    Alt(18),
    Meta(91),

    Escape(27),

    Space(32),
    PageUp(33),
    PageDown(34),
    End(35),
    Home(36),

    ArrowLeft(37),
    ArrowUp(38),
    ArrowRight(39),
    ArrowDown(40),

    Insert(45),
    Delete(46),

    Digit0(48),
    Digit1(49),
    Digit2(50),
    Digit3(51),
    Digit4(52),
    Digit5(53),
    Digit6(54),
    Digit7(55),
    Digit8(56),
    Digit9(57),
    
    // Alphanumeric Keys (A-Z, 0-9)
    KeyA(65),
    KeyB(66),
    KeyC(67),
    KeyD(68),
    KeyE(69),
    KeyF(70),
    KeyG(71),
    KeyH(72),
    KeyI(73),
    KeyJ(74),
    KeyK(75),
    KeyL(76),
    KeyM(77),
    KeyN(78),
    KeyO(79),
    KeyP(80),
    KeyQ(81),
    KeyR(82),
    KeyS(83),
    KeyT(84),
    KeyU(85),
    KeyV(86),
    KeyW(87),
    KeyX(88),
    KeyY(89),
    KeyZ(90),

    F1(112),
    F2(113),
    F3(114),
    F4(115),
    F5(116),
    F6(117),
    F7(118),
    F8(119),
    F9(120),
    F10(121),
    F11(122),
    F12(123);

    private final int windowsVirtualKeyCode;
    CdpKey(int windowsVirtualKeyCode) {
        this.windowsVirtualKeyCode = windowsVirtualKeyCode;
    }

    public int getModifier() {
        return switch (this) {
            case Alt -> 1;
            case Control -> 2;
            case Meta -> 4;
            case Shift -> 8;
            default -> 0;
        };
    }

    public String getText() {
        if(this.equals(CdpKey.Space))
            return " ";

        if(this.name().startsWith("Digit"))
            return name().substring(5);

        if(this.name().startsWith("Key"))
            return name().substring(3);

        if(this.name().startsWith("F"))
            return name().substring(1);

        return "";
    }

    public String getCode() {
        if(getModifier() > 0)
            return name() + "Left";
        return name();
    }

    public String getKey() {
        return getCode();
        //TODO
    }

    public int getWindowsVirtualKeyCode() {
        return windowsVirtualKeyCode;
    }

    public int getNativeVirtualKeyCode() {
        return windowsVirtualKeyCode;
    }

    public static CdpKey getCdpKey(char character){
        for(CdpKey key: CdpKey.values()) {
            if((key.windowsVirtualKeyCode >= 48 && key.windowsVirtualKeyCode <= 57) || (key.windowsVirtualKeyCode >= 65 && key.windowsVirtualKeyCode <= 90)) {
                char _character = key.name().replace("Key", "").replace("Digit", "").charAt(0);
                if(_character == character)
                    return key;
            }
        }
        return null;
    }
}

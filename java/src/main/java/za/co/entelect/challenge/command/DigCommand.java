package za.co.entelect.challenge.command;

public class DigCommand implements Command {

    private final int x;
    private final int y;

    public DigCommand(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String render() {
        return String.format("dig %d %d", x, y);
    }
//    Dig, koordinat block yang akan di dig format x, y : bisa diagonal yang penting 1 block jaraknya
}

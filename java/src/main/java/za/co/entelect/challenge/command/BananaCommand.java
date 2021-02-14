package za.co.entelect.challenge.command;

public class BananaCommand implements Command{
    private final int x;
    private final int y;

    public BananaCommand(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String render() {
        return String.format("banana %d %d", x, y);
    }

    // Melempar banana bomb ke koordinat x, y
    // Harus di cek terlebih dahulu jika terjangkau oleh worm
    // ID Worm Agent: 2
}
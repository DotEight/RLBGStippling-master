import java.util.Comparator;

public class DistanceToCellComparator implements Comparator<Cell> {
    private Cell c;

    public DistanceToCellComparator(Cell c)
    {
        this.c = c;
    }

    public int compare(Cell c1, Cell c2) {
        double dist1 = c.site.distanceTo(c1.site);
        double dist2 = c.site.distanceTo(c2.site);

        final double distDelta = dist1 - dist2;
        if (distDelta == 0)
            return 0;
        return distDelta < 0 ? -1 : 1;
    }
}

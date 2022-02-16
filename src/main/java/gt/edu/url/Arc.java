package gt.edu.url;

public class Arc<V, D> {
    private V Origin;
    private V Destiny;
    private Constraint<V, D> constraint;

    public Arc(V Origin, V Destiny, Constraint<V, D> constraint){
        this.Origin = Origin;
        this.Destiny = Destiny;
        this.constraint = constraint;
    }

    public V getOrigin() {
        return Origin;
    }

    public V getDestiny() { return Destiny; }

    public Constraint<V, D> getConstraint() {
        return constraint;
    }
}

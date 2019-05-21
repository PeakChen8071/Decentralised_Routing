package taxicomp;

public class Node {
    //understand the correct type to choose for Vertex and Edges.
    //https://jgrapht.org/guide/VertexAndEdgeTypes

    private final int id;

    public Node(int ID){
        id = ID;
    }
    public int getID(){
        return this.id;
    }


    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Node other = (Node) obj;
        return this.id == other.getID();
    }

    @Override
    public int hashCode() {
        int constant = 391441;
        return this.id*31+constant;
    }



    @Override
    public String toString() {
        return "Node " + id;
    }
}

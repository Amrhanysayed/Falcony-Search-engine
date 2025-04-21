import QueryProcessor.QueryProcessor;


public class Engine {
    Engine() { }

    public static void main(String[] args) throws Exception {
        QueryProcessor qp = new QueryProcessor();
        qp.process("\"Anas is the best\"");

    }


}

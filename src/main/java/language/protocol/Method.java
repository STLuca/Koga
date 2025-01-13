package language.protocol;

public class Method {

    String name;

    static class Parameter {
        String name;
        String type;
        boolean senderWritable;
        boolean receiverWritable;
    }

}

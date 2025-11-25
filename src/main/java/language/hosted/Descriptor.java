package language.hosted;

import java.util.ArrayList;

public class Descriptor {

    enum Type { Structure, Document, Generic }
    Type type;
    String name;
    ArrayList<Descriptor> subDescriptors = new ArrayList<>();

}

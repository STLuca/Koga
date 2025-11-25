package language.host;

import java.util.ArrayList;

public class Descriptor {

    enum Type { Structure, Document }
    Type type;
    String name;
    ArrayList<Descriptor> subDescriptors = new ArrayList<>();

}

package language.protocol;

import java.util.ArrayList;
import java.util.List;

public class Method {

    String name;
    ArrayList<Parameter> parameters = new ArrayList<>();

    static class Parameter {
        String size;
        Permission permission;
    }

    enum Permission {
        READ,
        WRITE,
        ALL
    }

}

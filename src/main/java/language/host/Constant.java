package language.host;

import java.util.List;

public class Constant {

    enum Type {
        String,
        Nums
    }

    Type type;
    String name;
    List<String> literals;

}

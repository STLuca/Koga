package machine;

import core.Instruction;

public interface Notifiable {

    void notify(Instruction.InterruptType interruptType, int instance, int interruptValue);

}

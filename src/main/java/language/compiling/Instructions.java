package language.compiling;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Should write all the use cases
 *  If
 *      2 addresses, append to both, cases and else
 *      1 address, add before for cases, add after for else (address must move though)
 *      always add before end, stop method calls after else?
 *  Switch
 *      jumps and cases, always add before, both start at the same time though and so use the same link
 *  Defer
 *
 */
public class Instructions {

    interface Item {
        Item insertBefore(InstructionBuilder ib);
        Item insertAfter(InstructionBuilder ib);
        Item addressBefore();
        Item addressAfter();
        void linkAfter(Item item);
        void linkBefore(Item item);

        default void remove() {};

        Optional<InstructionBuilder> prevIb();
        Optional<InstructionBuilder> nextIb();

        Optional<Item> instruction(List<InstructionBuilder> ibs);

    }

    class Start implements Item {

        Item next;

        public Start() {
            End end = new End();
            next = end;
            end.prev = this;
        }

        void insert(Item item) {

            item.linkBefore(this);
            item.linkAfter(next);
            next.linkBefore(item);
            this.linkAfter(item);
        }
        
        public Item insertBefore(InstructionBuilder ib) {
            IB item = new IB(ib);
            insert(item);
            return item;
        }
        
        public Item insertAfter(InstructionBuilder ib) {
            IB item = new IB(ib);
            insert(item);
            return item;
        }
        
        public Item addressBefore() {
            Address item = new Address();
            insert(item);
            return item;
        }
        
        public Item addressAfter() {
            Address item = new Address();
            insert(item);
            return item;
        }
        
        public void linkAfter(Item item) {
            next = item;
        }
        
        public void linkBefore(Item item) {
            throw new RuntimeException("Should never be called");
        }
        
        public Optional<InstructionBuilder> prevIb() {
            return Optional.empty();
        }
        
        public Optional<InstructionBuilder> nextIb() {
            return next.nextIb();
        }

        public Optional<Item> instruction(List<InstructionBuilder> ibs) {
            return Optional.of(next);
        }
    }

    class End implements Item {

        Item prev;

        void insert(Item item) {
            item.linkBefore(prev);
            item.linkAfter(this);
            prev.linkAfter(item);
            this.linkBefore(item);
        }
        
        public Item insertBefore(InstructionBuilder ib) {
            IB item = new IB(ib);
            insert(item);
            return item;
        }
        
        public Item insertAfter(InstructionBuilder ib) {
            IB item = new IB(ib);
            insert(item);
            return item;
        }
        
        public Item addressBefore() {
            Address item = new Address();
            insert(item);
            return item;
        }
        
        public Item addressAfter() {
            Address item = new Address();
            insert(item);
            return item;
        }
        
        public void linkAfter(Item item) {
            throw new RuntimeException("Should never be called");
        }
        
        public void linkBefore(Item item) {
            prev = item;
        }
        
        public Optional<InstructionBuilder> prevIb() {
            return prev.prevIb();
        }
        
        public Optional<InstructionBuilder> nextIb() {
            return Optional.empty();
        }
        
        public Optional<Item> instruction(List<InstructionBuilder> ibs) {
            return Optional.empty();
        }
    }

    class IB implements Item {

        public IB(InstructionBuilder ib) {
            this.ib = ib;
        }

        InstructionBuilder ib;
        Item prev;
        Item next;

        void insertBefore(Item item) {
            item.linkBefore(prev);
            item.linkAfter(this);
            prev.linkAfter(item);
            this.linkBefore(item);
        }

        void insertAfter(Item item) {
            item.linkBefore(this);
            item.linkAfter(next);
            next.linkBefore(item);
            this.linkAfter(item);
        }
        
        public Item insertBefore(InstructionBuilder ib) {
            IB item = new IB(ib);
            insertBefore(item);
            return item;
        }
        
        public Item insertAfter(InstructionBuilder ib) {
            IB item = new IB(ib);
            insertAfter(item);
            return item;
        }
        
        public Item addressBefore() {
            Address item = new Address();
            insertBefore(item);
            return item;
        }
        
        public Item addressAfter() {
            Address item = new Address();
            insertAfter(item);
            return item;
        }
        
        public void linkAfter(Item item) {
            next = item;
        }
        
        public void linkBefore(Item item) {
            prev = item;
        }
        
        public Optional<InstructionBuilder> prevIb() {
            return Optional.of(ib);
        }
        
        public Optional<InstructionBuilder> nextIb() {
            return Optional.of(ib);
        }
        
        public Optional<Item> instruction(List<InstructionBuilder> ibs) {
            ibs.add(ib);
            return Optional.of(next);
        }
    }

    class Address implements Item {

        Item prev;
        Item next;

        void insertBefore(Item item) {
            item.linkBefore(prev);
            item.linkAfter(this);
            prev.linkAfter(item);
            this.linkBefore(item);
        }

        void insertAfter(Item item) {
            item.linkBefore(this);
            item.linkAfter(next);
            next.linkBefore(item);
            this.linkAfter(item);
        }
        
        public Item insertBefore(InstructionBuilder ib) {
            IB item = new IB(ib);
            insertBefore(item);
            return item;
        }
        
        public Item insertAfter(InstructionBuilder ib) {
            IB item = new IB(ib);
            insertAfter(item);
            return item;
        }
        
        public Item addressBefore() {
            Address item = new Address();
            insertBefore(item);
            return item;
        }
        
        public Item addressAfter() {
            Address item = new Address();
            insertAfter(item);
            return item;
        }
        
        public void linkAfter(Item item) {
            next = item;
        }
        
        public void linkBefore(Item item) {
            prev = item;
        }
        
        public void remove() {
            prev.linkAfter(next);
            next.linkBefore(prev);
            prev = null;
            next = null;
        }
        
        public Optional<InstructionBuilder> prevIb() {
            return prev.prevIb();
        }
        
        public Optional<InstructionBuilder> nextIb() {
            return next.nextIb();
        }
        
        public Optional<Item> instruction(List<InstructionBuilder> ibs) {
            return Optional.of(next);
        }
    }

    Item start;

    public Instructions() {
        start = new Start();
    }

    List<InstructionBuilder> list() {
        ArrayList<InstructionBuilder> instructions = new ArrayList<>();
        Item curr = start;
        while (curr != null) {
            curr = curr.instruction(instructions).orElse(null);
        }
        return instructions;
    }

}

package converter.utils;

import converter.automaton.InformationWrapper;
import main.LTLfAutomatonResultWrapper;
import net.sf.tweety.logics.pl.syntax.PropositionalSignature;
import rationals.Automaton;
import rationals.State;
import rationals.Synchronization;
import rationals.Transition;
import rationals.transformations.Complement;
import rationals.transformations.Mix;
import rationals.transformations.Pruner;
import rationals.transformations.Reducer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by arnelaponin on 08/11/2016.
 */
public class AutomatonOperationUtils {

    public static LTLfAutomatonResultWrapper createDefaultLtlAutomaton(PropositionalSignature signature, String ltlFormula) {
        boolean declare = true;
        boolean minimize = true;
        boolean trim = false;
        boolean noEmptyTrace = true;
        boolean printing = false;
        return main.Main.ltlfString2Aut(ltlFormula, signature, declare, minimize, trim, noEmptyTrace, printing);
    }

    public static Automaton getIntersection(Automaton a1, Automaton a2) {
        return new Mix(new Synchronization() {
            //Taken from the default one.
            public Object synchronize(Object t1, Object t2) {
                return t1 == null?null:(t1.equals(t2)?t1:null);
            }

            //Takes an union of alphabets, so that all of the members would be synchronized.
            public Set synchronizable(Set set, Set set1) {
                Set<String> setUnion = new HashSet<String>(set);
                setUnion.addAll(set1);
                return setUnion;
            }

            public Set synchronizing(Collection collection) {
                return null;
            }

            public boolean synchronizeWith(Object o, Set set) {
                return false;
            }
        }).transform(a1, a2);
    }

    public static Automaton getReduced(Automaton a) {
        return new Reducer().transform(a);
    }

    public static Automaton getTrimmed(Automaton a) {
        return new Pruner().transform(a);
    }

    public static Automaton getRelativeComplement(Automaton a1, Automaton a2) {
        Automaton negA2 = new Complement().transform(a2);
        return AutomatonOperationUtils.getIntersection(a1, negA2);
    }

    public static Automaton getNegated(Automaton a) {
        return new Complement().transform(a);
    }

    public static void colorAutomatonStates(InformationWrapper informationWrapper, String fileName) {
        Map<State, List<Transition>> badStatesMap = informationWrapper.getBadStatesWithTransitions();
        Set<State> badStates = badStatesMap.keySet();
        List<List<Transition>> badTransitionsList = new ArrayList(badStatesMap.values());
        //List<Transition> badTransitions = badTransitionsList.stream().flatMap(List::stream).collect(Collectors.toList());
        List<Transition> badTransitions = new ArrayList<>();
        for (List<Transition> badList : badTransitionsList) {
            badTransitions.addAll(badList);
        }

        Set<State> semiBadStates = informationWrapper.getSemiBadStates().keySet();
        List<List<Transition>> troubledTransitionsList = new ArrayList(informationWrapper.getSemiBadStates().values());

        List<Transition> semiBadTransitions = new ArrayList<>();
        for (List<Transition> semiBadList : troubledTransitionsList) {
            semiBadTransitions.addAll(semiBadList);
        }
        //List<Transition> semiBadTransitions = troubledTransitionsList.stream().flatMap(List::stream).collect(Collectors.toList());

        //TODO:Remove this.
        Automaton trimIntersection = AutomatonOperationUtils.getTrimmed(informationWrapper.getReducedIntersection());
        utils.AutomatonUtils.printAutomaton(trimIntersection, "automatons/automaton_trim.gv");

        drawColoredAutomaton(informationWrapper.getReducedIntersection(), fileName, badStates, badTransitions, semiBadStates, semiBadTransitions);
    }

    private static void drawColoredAutomaton(Automaton automaton, String fileName, Set<State> badPlaces, List<Transition> badTransitions, Set<State> semiBadStates, List<Transition> semiBadTransitions) {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        PrintStream ps = new PrintStream(fos);
        ps.println(toDot(automaton, badPlaces, badTransitions, semiBadStates, semiBadTransitions));
        ps.flush();
        ps.close();
    }

    public static void drawAutomaton(Automaton automaton, String fileName) {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        PrintStream ps = new PrintStream(fos);
        ps.println(toDot(automaton));
        ps.flush();
        ps.close();
    }

    private static String toDot(Automaton automaton) {
        Set<State> badPlaces = new HashSet<>();
        List<Transition> badTransitions = new ArrayList<>();
        Set<State> semiBadStates = new HashSet<>();
        List<Transition> semiBadTransitions = new ArrayList<>();
        return toDot(automaton, badPlaces, badTransitions, semiBadStates, semiBadTransitions);
    }

    private static String toDot(Automaton automaton, Set<State> badPlaces, List<Transition> badTransitions, Set<State> semiBadStates, List<Transition> semiBadTransitions) {
        StringBuilder graphString = new StringBuilder("digraph Automaton {\n");
        graphString.append("  rankdir = LR;\n");
        Set<State> states = automaton.states();
        for (State state : states) {

            String stateString = state.toString();
            graphString.append("  ").append(stateString);
            if (state.isTerminal()) {
                graphString.append(" [shape=doublecircle,label=\"" + stateString + "\"");
                graphString.append("];\n");
            } else {
                graphString.append(" [shape=circle,label=\"" + stateString + "\"");

                if (semiBadStates.contains(state)) {
                    graphString.append(",style=filled,fillcolor=\"gold\"");
                } else if (badPlaces.contains(state)) {
                    graphString.append(",style=filled,fillcolor=\"red\"");
                }
                graphString.append("];\n");
            }
            if (state.isInitial()) {
                graphString.append("  initial [shape=plaintext,label=\"" + stateString + "\"];\n");
                graphString.append("  initial -> ").append(stateString).append("\n");
            }
            Set<Transition> transitions = automaton.delta(state);
            for (Transition t : transitions) {
                graphString.append("  ").append((t.start()).toString());
                State arrivalState = t.end();
                graphString.append(" -> ").append(arrivalState.toString()).append(" [label=\"");

                try {
                    graphString.append(t.label().toString());
                } catch (NullPointerException e) {
                    graphString.append("");
                }
                if (semiBadTransitions.contains(t)) {
                    graphString.append("\",color=\"gold");
                } else if (badTransitions.contains(t)) {
                    graphString.append("\",color=\"red");
                }
                graphString.append("\"]");
                graphString.append("\n");
            }
        }
        return graphString.append("}\n").toString();
    }


}

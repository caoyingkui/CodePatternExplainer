package extractor;

import dataStructure.PatternInstance;
import dataStructure.PatternInstanceLine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatternInstanceFilter {

    private static List<PatternInstance> illegalInstanceFilter(List<PatternInstance> patternInstances) {
        List<PatternInstance> legalPatternInstances = new ArrayList<>();

        for (PatternInstance instance : patternInstances) {
            if (!instance.isIllegalPattern()) {
                legalPatternInstances.add(instance);
            }
        }

        return legalPatternInstances;
    }

    private static List<PatternInstance> normalFilter(List<PatternInstance> patternInstances) {
        Map<String, Integer> signatureCounts = new HashMap<>();

        for (PatternInstance instance : patternInstances) {
            String signature = instance.getSignature();
            if (signatureCounts.containsKey(signature)) {
                signatureCounts.put(signature, signatureCounts.get(signature) + 1);
            } else {
                signatureCounts.put(signature, 1);
            }
        }

        String normalSignature = null;
        int normalSignatureCount = 0;
        for (String signature : signatureCounts.keySet()) {
            int signatureCount = signatureCounts.get(signature);
            if (signatureCount > normalSignatureCount) {
                normalSignatureCount = signatureCount;
                normalSignature = signature;
            }
        }

        List<PatternInstance> normalPatternInstances = new ArrayList<>();
        for (PatternInstance instance : patternInstances) {
            if (instance.getSignature().equals(normalSignature)) {
                normalPatternInstances.add(instance);
            }
        }
        return normalPatternInstances;
    }

    public static List<PatternInstance> filter(List<PatternInstance> patternInstances) {
        System.out.println("------------------------------------");
        System.out.println("Before Filter : PatternInstance Number = " + patternInstances.size());

        List<PatternInstance> legalPatternInstances = illegalInstanceFilter(patternInstances);
        System.out.println("After Illegal Filter : PatternInstance Number = " + legalPatternInstances.size());

        List<PatternInstance> normalPatternInstances = normalFilter(legalPatternInstances);
        System.out.println("After Normal Filter : PatternInstance Number = " + normalPatternInstances.size());

        System.out.println("Filter Process Done");
        System.out.println("------------------------------------");
        return normalPatternInstances;
    }
}

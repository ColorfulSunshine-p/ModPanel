package com.modpanel.compatibility;

import java.util.ArrayList;
import java.util.List;

public class VersionMatcher {
    public boolean matches(String actual, String min, String max) {
        if (actual == null) return false;
        if (min != null && compare(actual, min) < 0) return false;
        if (max != null && compare(actual, max) > 0) return false;
        return true;
    }

    public int compare(String a, String b) {
        List<Integer> pa = core(a), pb = core(b);
        int n = Math.max(pa.size(), pb.size());
        for (int i = 0; i < n; i++) {
            int va = i < pa.size() ? pa.get(i) : 0;
            int vb = i < pb.size() ? pb.get(i) : 0;
            if (va != vb) return Integer.compare(va, vb);
        }
        String sa = suffix(a), sb = suffix(b);
        if (sa.isEmpty() && !sb.isEmpty()) return 1;
        if (!sa.isEmpty() && sb.isEmpty()) return -1;
        return sa.compareToIgnoreCase(sb);
    }

    /** Numeric core of a version, build metadata such as {@code +mc1.21.1} is ignored. */
    private List<Integer> core(String version) {
        List<Integer> out = new ArrayList<>();
        if (version == null) return out;
        StringBuilder digits = new StringBuilder();
        for (char c : withoutMetadata(version).toCharArray()) {
            if (Character.isDigit(c) || c == '.') digits.append(c);
            else break;
        }
        for (String part : digits.toString().split("[^0-9]+")) {
            if (!part.isEmpty()) try { out.add(Integer.parseInt(part)); } catch (NumberFormatException ignored) { }
        }
        return out;
    }

    private String suffix(String version) {
        if (version == null) return "";
        String s = withoutMetadata(version);
        int i = 0;
        while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
        return i < s.length() ? s.substring(i).trim() : "";
    }

    private String withoutMetadata(String version) {
        int plus = version.indexOf('+');
        return plus < 0 ? version : version.substring(0, plus);
    }
}

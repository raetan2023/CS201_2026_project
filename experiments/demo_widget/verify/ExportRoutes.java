package com.delivery.widget;

import com.delivery.routing.SolverB;
import com.delivery.tools.BruteForce;
import com.delivery.tools.Instance;
import com.delivery.tools.NearestNeighbor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;

/**
 * Runs the project's real Java on input files listed by verify.py, one path per line.
 * Prints one line per file: "SolverB route|NearestNeighbor route|BruteForce route", ids 1..n.
 */
public class ExportRoutes {

    public static void main(String[] args) throws IOException {
        for (String line : Files.readAllLines(Path.of(args[0]), StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            Instance in = Instance.read(Path.of(line));
            List<Integer> b = new SolverB().solve(in.toSolverInstance()).route().stream()
                    .map(id -> id + 1)   // solver customer i = instance customer i + 1
                    .toList();
            System.out.println(ids(b) + "|" + ids(NearestNeighbor.solve(in)) + "|" + ids(BruteForce.solve(in)));
        }
    }

    private static String ids(List<Integer> route) {
        StringJoiner out = new StringJoiner(",");
        for (int id : route) out.add(Integer.toString(id));
        return out.toString();
    }
}

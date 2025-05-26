package com.example.diploma.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RouteCalculator {

    private static final String USER_AGENT = "MyTransportApp/1.0 (myemail@example.com)";


    public static double[] geocode(String address, String street, String city, String country)
            throws IOException, InterruptedException {
        double[] result = geocodeFree(address);
        if (result != null) {
            return result;
        }
        return geocodeStructured(street, city, country);
    }


    private static double[] geocodeFree(String address)
            throws IOException, InterruptedException {
        String q = URLEncoder.encode(address + ", Россия", StandardCharsets.UTF_8);
        String url = String.format(
                "https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=ru&accept-language=ru&q=%s",
                q
        );
        JSONArray arr = sendAndParse(url);
        if (arr.isEmpty()) return null;
        JSONObject obj = arr.getJSONObject(0);
        return new double[]{ obj.getDouble("lat"), obj.getDouble("lon") };
    }

    private static double[] geocodeStructured(String street, String city, String country)
            throws IOException, InterruptedException {
        String params = String.format(
                "street=%s&city=%s&country=%s",
                URLEncoder.encode(street,  StandardCharsets.UTF_8),
                URLEncoder.encode(city,    StandardCharsets.UTF_8),
                URLEncoder.encode(country, StandardCharsets.UTF_8)
        );
        String url = String.format(
                "https://nominatim.openstreetmap.org/search?format=json&limit=1&accept-language=ru&%s",
                params
        );
        JSONArray arr = sendAndParse(url);
        if (arr.isEmpty()) {
            throw new IllegalArgumentException(
                    "Адрес не найден ни свободным, ни структурированным запросом");
        }
        JSONObject obj = arr.getJSONObject(0);
        return new double[]{ obj.getDouble("lat"), obj.getDouble("lon") };
    }


    private static JSONArray sendAndParse(String url)
            throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "ru")
                .GET()
                .build();
        HttpResponse<String> resp = HttpClient.newHttpClient()
                .send(req, HttpResponse.BodyHandlers.ofString());
        return new JSONArray(resp.body());
    }

    public static class RouteInfo {
        public final double distanceMeters;
        public final double durationSeconds;
        public RouteInfo(double d, double t) {
            this.distanceMeters = d;
            this.durationSeconds = t;
        }

    }

    public static RouteInfo getRoute(double[] origin, double[] destination)
            throws IOException, InterruptedException {
        String coords = String.format(Locale.US,
                "%f,%f;%f,%f",
                origin[1], origin[0],
                destination[1], destination[0]
        );
        String url = "https://router.project-osrm.org/route/v1/driving/"
                + coords + "?overview=false";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        HttpResponse<String> resp = HttpClient.newHttpClient()
                .send(req, HttpResponse.BodyHandlers.ofString());
        JSONObject root = new JSONObject(resp.body());
        if (!"Ok".equals(root.getString("code"))) {
            throw new RuntimeException("OSRM error: " + root.getString("code"));
        }
        JSONObject route = root.getJSONArray("routes").getJSONObject(0);
        return new RouteInfo(
                route.getDouble("distance"),
                route.getDouble("duration")
        );
    }

    public static class OptimizedRouteInfo {
        public final List<Integer> waypointOrder;
        public final double totalDistanceMeters;
        public final double totalDurationSeconds;
        public OptimizedRouteInfo(List<Integer> waypointOrder,
                                  double totalDistanceMeters,
                                  double totalDurationSeconds) {
            this.waypointOrder = waypointOrder;
            this.totalDistanceMeters = totalDistanceMeters;
            this.totalDurationSeconds = totalDurationSeconds;
        }
    }

    /**
     * Возвращает оптимизированный маршрут по набору адресов:
     * геокодим каждый через geocode(...),
     * затем Trip API выдаёт оптимальный порядок и суммарное расстояние/время.
     *
     * @param addresses массив русскоязычных адресов в порядке ввода
     * @param city      город для геокодинга, например "Москва"
     * @param country   страна, например "Россия"
     */
    public static OptimizedRouteInfo getOptimizedRoute(
            List<String> addresses,
            String city,
            String country
    ) throws IOException, InterruptedException {

        List<double[]> points = new ArrayList<>();
        for (String addr : addresses) {
            points.add(geocode(addr, addr, city, country));
        }


        String coordParam = points.stream()
                .map(p -> String.format(Locale.US, "%f,%f", p[1], p[0]))
                .collect(Collectors.joining(";"));

        String url = "https://router.project-osrm.org/trip/v1/driving/"
                + coordParam
                + "?source=first&destination=last&roundtrip=false&overview=false";
        System.out.println("OSRM Trip URL: " + url);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        HttpResponse<String> resp = HttpClient.newHttpClient()
                .send(req, HttpResponse.BodyHandlers.ofString());

        JSONObject root = new JSONObject(resp.body());
        if (!"Ok".equals(root.getString("code"))) {
            throw new RuntimeException("OSRM Trip error: " + root.getString("code"));
        }


        JSONArray wps = root.getJSONArray("waypoints");
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < wps.length(); i++) {
            order.add(wps.getJSONObject(i).getInt("waypoint_index"));
        }

        JSONObject trip = root.getJSONArray("trips").getJSONObject(0);
        double dist = trip.getDouble("distance");
        double dur  = trip.getDouble("duration");

        return new OptimizedRouteInfo(order, dist, dur);
    }

    public static RouteInfo getOrderedRouteInfo(List<String> addresses, String city, String country)
            throws IOException, InterruptedException {

        List<double[]> points = new ArrayList<>();
        for (String addr : addresses) {
            points.add(geocode(addr, addr, city, country));
        }


        double totalDistance = 0;
        double totalDuration = 0;

        for (int i = 0; i < points.size() - 1; i++) {
            double[] from = points.get(i);
            double[] to   = points.get(i + 1);
            RouteInfo info = getRoute(from, to);
            totalDistance += info.distanceMeters;
            totalDuration += info.durationSeconds;
        }

        return new RouteInfo(totalDistance, totalDuration);
    }
}


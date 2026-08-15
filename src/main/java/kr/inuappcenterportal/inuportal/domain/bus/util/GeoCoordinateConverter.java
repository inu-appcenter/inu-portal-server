package kr.inuappcenterportal.inuportal.domain.bus.util;

/**
 * 인천 BIS / 공공데이터포털 Bessel 1841 TM 중부원점 (EPSG:5181 / EPSG:5174) 좌표를
 * 7-Parameter Bursa-Wolf Datum 변환을 적용하여 WGS84 표준 위경도(EPSG:4326)로 정밀 변환하는 유틸리티
 */
public class GeoCoordinateConverter {

    // Bessel 1841 타원체 파라미터
    private static final double a_b = 6377397.155;
    private static final double f_b = 1.0 / 299.1528128;
    private static final double b_b = a_b * (1.0 - f_b);
    private static final double e2_b = (a_b * a_b - b_b * b_b) / (a_b * a_b);
    private static final double ePrime2_b = (a_b * a_b - b_b * b_b) / (b_b * b_b);

    // EPSG:5181 (Daum / 인천 BIS TM 중부원점)
    private static final double lat0 = Math.toRadians(38.0);
    private static final double lng0 = Math.toRadians(127.0);
    private static final double k0 = 1.0;
    private static final double falseE = 200000.0;
    private static final double falseN = 500000.0;

    // 7-Parameter Bursa-Wolf Datum Shift (Tokyo/Bessel -> WGS84)
    private static final double dx = -145.907;
    private static final double dy = 505.034;
    private static final double dz = 685.756;
    private static final double rx = Math.toRadians(-1.162 / 3600.0);
    private static final double ry = Math.toRadians(2.347 / 3600.0);
    private static final double rz = Math.toRadians(1.592 / 3600.0);
    private static final double ds = 6.342e-6;

    // WGS84 타원체 파라미터
    private static final double a_w = 6378137.0;
    private static final double f_w = 1.0 / 298.257223563;
    private static final double b_w = a_w * (1.0 - f_w);
    private static final double e2_w = (a_w * a_w - b_w * b_w) / (a_w * a_w);
    private static final double ePrime2_w = (a_w * a_w - b_w * b_w) / (b_w * b_w);

    /**
     * TM (x=POSX, y=POSY) 좌표를 WGS84 [위도, 경도]로 정밀 변환합니다.
     * 이미 WGS84 좌표 범위(위도 30~45, 경도 120~135)인 경우 그대로 반환합니다.
     */
    public static double[] tmToWgs84(Double x, Double y) {
        if (x == null || y == null) {
            return new double[]{0.0, 0.0};
        }

        // 이미 WGS84 위경도인 경우
        if (y >= 30.0 && y <= 45.0 && x >= 120.0 && x <= 135.0) {
            return new double[]{y, x};
        }
        if (x >= 30.0 && x <= 45.0 && y >= 120.0 && y <= 135.0) {
            return new double[]{x, y};
        }

        double x_ = x - falseE;
        double y_ = y - falseN;

        double e4 = e2_b * e2_b;
        double e6 = e4 * e2_b;

        double M0 = a_b * ((1.0 - e2_b / 4.0 - 3.0 * e4 / 64.0 - 5.0 * e6 / 256.0) * lat0
                - (3.0 * e2_b / 8.0 + 3.0 * e4 / 32.0 + 45.0 * e6 / 1024.0) * Math.sin(2.0 * lat0)
                + (15.0 * e4 / 256.0 + 45.0 * e6 / 1024.0) * Math.sin(4.0 * lat0)
                - (35.0 * e6 / 3072.0) * Math.sin(6.0 * lat0));

        double M = M0 + y_ / k0;
        double mu = M / (a_b * (1.0 - e2_b / 4.0 - 3.0 * e4 / 64.0 - 5.0 * e6 / 256.0));

        double e1 = (1.0 - Math.sqrt(1.0 - e2_b)) / (1.0 + Math.sqrt(1.0 - e2_b));
        double J1 = (3.0 * e1 / 2.0 - 27.0 * Math.pow(e1, 3) / 32.0);
        double J2 = (21.0 * e1 * e1 / 16.0 - 55.0 * Math.pow(e1, 4) / 32.0);
        double J3 = (151.0 * Math.pow(e1, 3) / 96.0);
        double J4 = (1097.0 * Math.pow(e1, 4) / 512.0);

        double fp = mu + J1 * Math.sin(2.0 * mu) + J2 * Math.sin(4.0 * mu) + J3 * Math.sin(6.0 * mu) + J4 * Math.sin(8.0 * mu);

        double C1 = ePrime2_b * Math.pow(Math.cos(fp), 2);
        double T1 = Math.pow(Math.tan(fp), 2);
        double R1 = a_b * (1.0 - e2_b) / Math.pow(1.0 - e2_b * Math.pow(Math.sin(fp), 2), 1.5);
        double N1 = a_b / Math.sqrt(1.0 - e2_b * Math.pow(Math.sin(fp), 2));
        double D = x_ / (N1 * k0);

        double besselLat = fp - (N1 * Math.tan(fp) / R1) * (D * D / 2.0 - (5.0 + 3.0 * T1 + 10.0 * C1 - 4.0 * C1 * C1 - 9.0 * ePrime2_b) * Math.pow(D, 4) / 24.0
                + (61.0 + 90.0 * T1 + 298.0 * C1 + 45.0 * T1 * T1 - 252.0 * ePrime2_b - 3.0 * C1 * C1) * Math.pow(D, 6) / 720.0);

        double besselLng = lng0 + (D - (1.0 + 2.0 * T1 + C1) * Math.pow(D, 3) / 6.0
                + (5.0 - 2.0 * C1 + 28.0 * T1 - 3.0 * C1 * C1 + 8.0 * ePrime2_b + 24.0 * T1 * T1) * Math.pow(D, 5) / 120.0) / Math.cos(fp);

        // 2. Bessel Geodetic -> ECEF
        double h = 0.0;
        double sinLat = Math.sin(besselLat);
        double cosLat = Math.cos(besselLat);
        double sinLng = Math.sin(besselLng);
        double cosLng = Math.cos(besselLng);

        double N = a_b / Math.sqrt(1.0 - e2_b * sinLat * sinLat);
        double X_b = (N + h) * cosLat * cosLng;
        double Y_b = (N + h) * cosLat * sinLng;
        double Z_b = (N * (1.0 - e2_b) + h) * sinLat;

        // 3. 7-Parameter Bursa-Wolf Datum Shift to WGS84 ECEF
        double X_w = (1.0 + ds) * (X_b + rz * Y_b - ry * Z_b) + dx;
        double Y_w = (1.0 + ds) * (-rz * X_b + Y_b + rx * Z_b) + dy;
        double Z_w = (1.0 + ds) * (ry * X_b - rx * Y_b + Z_b) + dz;

        // 4. WGS84 ECEF -> WGS84 Geodetic
        double p = Math.sqrt(X_w * X_w + Y_w * Y_w);
        double theta = Math.atan2(Z_w * a_w, p * b_w);

        double wgsLat = Math.atan2(
                Z_w + ePrime2_w * b_w * Math.pow(Math.sin(theta), 3),
                p - e2_w * a_w * Math.pow(Math.cos(theta), 3)
        );
        double wgsLng = Math.atan2(Y_w, X_w);

        return new double[]{Math.toDegrees(wgsLat), Math.toDegrees(wgsLng)};
    }
}

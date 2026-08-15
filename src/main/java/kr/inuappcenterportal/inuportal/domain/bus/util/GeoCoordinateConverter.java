package kr.inuappcenterportal.inuportal.domain.bus.util;

/**
 * 인천 BIS / 공공데이터포털 GRS80 TM 중부원점 (EPSG:5181) 좌표를
 * 카카오맵 및 WGS84 표준 위경도(EPSG:4326)로 변환하는 유틸리티
 */
public class GeoCoordinateConverter {

    private static final double a = 6378137.0; // GRS80 타원체 장반경
    private static final double f = 1.0 / 298.257222101; // 편평률
    private static final double b = a * (1.0 - f);
    private static final double e2 = (a * a - b * b) / (a * a);
    private static final double ePrime2 = (a * a - b * b) / (b * b);

    // TM 중부원점 (EPSG:5181) 파라미터
    private static final double lat0 = Math.toRadians(38.0);
    private static final double lng0 = Math.toRadians(127.0);
    private static final double k0 = 1.0;
    private static final double falseE = 200000.0;
    private static final double falseN = 500000.0;

    /**
     * TM (x=POSX, y=POSY) 좌표를 WGS84 [위도, 경도]로 변환합니다.
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

        double e4 = e2 * e2;
        double e6 = e4 * e2;

        double M0 = a * ((1.0 - e2 / 4.0 - 3.0 * e4 / 64.0 - 5.0 * e6 / 256.0) * lat0
                - (3.0 * e2 / 8.0 + 3.0 * e4 / 32.0 + 45.0 * e6 / 1024.0) * Math.sin(2.0 * lat0)
                + (15.0 * e4 / 256.0 + 45.0 * e6 / 1024.0) * Math.sin(4.0 * lat0)
                - (35.0 * e6 / 3072.0) * Math.sin(6.0 * lat0));

        double M = M0 + y_ / k0;
        double mu = M / (a * (1.0 - e2 / 4.0 - 3.0 * e4 / 64.0 - 5.0 * e6 / 256.0));

        double e1 = (1.0 - Math.sqrt(1.0 - e2)) / (1.0 + Math.sqrt(1.0 - e2));
        double J1 = (3.0 * e1 / 2.0 - 27.0 * Math.pow(e1, 3) / 32.0);
        double J2 = (21.0 * e1 * e1 / 16.0 - 55.0 * Math.pow(e1, 4) / 32.0);
        double J3 = (151.0 * Math.pow(e1, 3) / 96.0);
        double J4 = (1097.0 * Math.pow(e1, 4) / 512.0);

        double fp = mu + J1 * Math.sin(2.0 * mu) + J2 * Math.sin(4.0 * mu) + J3 * Math.sin(6.0 * mu) + J4 * Math.sin(8.0 * mu);

        double C1 = ePrime2 * Math.pow(Math.cos(fp), 2);
        double T1 = Math.pow(Math.tan(fp), 2);
        double R1 = a * (1.0 - e2) / Math.pow(1.0 - e2 * Math.pow(Math.sin(fp), 2), 1.5);
        double N1 = a / Math.sqrt(1.0 - e2 * Math.pow(Math.sin(fp), 2));
        double D = x_ / (N1 * k0);

        double lat = fp - (N1 * Math.tan(fp) / R1) * (D * D / 2.0 - (5.0 + 3.0 * T1 + 10.0 * C1 - 4.0 * C1 * C1 - 9.0 * ePrime2) * Math.pow(D, 4) / 24.0
                + (61.0 + 90.0 * T1 + 298.0 * C1 + 45.0 * T1 * T1 - 252.0 * ePrime2 - 3.0 * C1 * C1) * Math.pow(D, 6) / 720.0);

        double lng = lng0 + (D - (1.0 + 2.0 * T1 + C1) * Math.pow(D, 3) / 6.0
                + (5.0 - 2.0 * C1 + 28.0 * T1 - 3.0 * C1 * C1 + 8.0 * ePrime2 + 24.0 * T1 * T1) * Math.pow(D, 5) / 120.0) / Math.cos(fp);

        return new double[]{Math.toDegrees(lat), Math.toDegrees(lng)};
    }
}

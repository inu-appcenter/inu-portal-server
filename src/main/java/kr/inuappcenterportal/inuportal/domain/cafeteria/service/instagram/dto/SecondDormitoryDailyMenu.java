package kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.dto;

public record SecondDormitoryDailyMenu(
        String lunchMenu,
        String dinnerMenu
) {

    public static SecondDormitoryDailyMenu empty() {
        return new SecondDormitoryDailyMenu(null, null);
    }

    public boolean hasLunch() {
        return lunchMenu != null && !lunchMenu.isBlank();
    }

    public boolean hasDinner() {
        return dinnerMenu != null && !dinnerMenu.isBlank();
    }

    public boolean hasAnyMenu() {
        return hasLunch() || hasDinner();
    }
}

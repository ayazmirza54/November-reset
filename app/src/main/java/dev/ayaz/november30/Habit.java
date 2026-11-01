package dev.ayaz.november30;

/** One of the 13 daily challenge habits. */
final class Habit {
    enum Kind { CHECK, NUMBER, TEXT, WORKOUT }

    final int id;
    final String emoji;
    final String title;
    final String rule;
    final Kind kind;
    final String unit;
    final boolean decimal;

    Habit(int id, String emoji, String title, String rule, Kind kind, String unit, boolean decimal) {
        this.id = id;
        this.emoji = emoji;
        this.title = title;
        this.rule = rule;
        this.kind = kind;
        this.unit = unit;
        this.decimal = decimal;
    }

    static Habit check(int id, String emoji, String title, String rule) {
        return new Habit(id, emoji, title, rule, Kind.CHECK, null, false);
    }

    static Habit number(int id, String emoji, String title, String rule, String unit, boolean decimal) {
        return new Habit(id, emoji, title, rule, Kind.NUMBER, unit, decimal);
    }

    static Habit text(int id, String emoji, String title, String rule) {
        return new Habit(id, emoji, title, rule, Kind.TEXT, null, false);
    }
}

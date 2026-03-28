import React from "react";
import { StyleSheet, Text, View } from "react-native";
import { colors } from "../theme/colors";

interface StatusPillProps {
  label: string;
  value: string;
  tone?: "ok" | "warn" | "danger" | "neutral";
}

const toneMap = {
  ok: { bg: "rgba(32, 201, 151, 0.2)", border: colors.accentMain, text: colors.accentMain },
  warn: { bg: "rgba(255, 209, 102, 0.2)", border: colors.warning, text: colors.warning },
  danger: { bg: "rgba(255, 107, 107, 0.2)", border: colors.danger, text: colors.danger },
  neutral: { bg: "rgba(75, 98, 115, 0.3)", border: colors.inactive, text: colors.textSecondary },
};

export function StatusPill({ label, value, tone = "neutral" }: StatusPillProps) {
  const palette = toneMap[tone];
  return (
    <View style={[styles.pill, { backgroundColor: palette.bg, borderColor: palette.border }]}>
      <Text style={styles.label}>{label}</Text>
      <Text style={[styles.value, { color: palette.text }]}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  pill: {
    borderWidth: 1,
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 6,
    flexDirection: "row",
    gap: 6,
    alignItems: "center",
  },
  label: {
    color: colors.textSecondary,
    fontSize: 12,
    fontWeight: "600",
  },
  value: {
    fontSize: 12,
    fontWeight: "700",
  },
});


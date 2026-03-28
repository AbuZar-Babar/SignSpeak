import React from "react";
import { StyleSheet, Text, View } from "react-native";
import { colors } from "../theme/colors";

interface PlaceholderScreenProps {
  title: string;
  points: string[];
}

export function PlaceholderScreen({ title, points }: PlaceholderScreenProps) {
  return (
    <View style={styles.wrapper}>
      <Text style={styles.title}>{title}</Text>
      <Text style={styles.subtitle}>Placeholder for next phase</Text>
      <View style={styles.card}>
        {points.map((point) => (
          <Text key={point} style={styles.point}>
            - {point}
          </Text>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    flex: 1,
    padding: 16,
    gap: 10,
  },
  title: {
    color: colors.textPrimary,
    fontWeight: "800",
    fontSize: 22,
  },
  subtitle: {
    color: colors.textSecondary,
    fontSize: 14,
    marginBottom: 8,
  },
  card: {
    backgroundColor: colors.cardBg,
    borderColor: colors.border,
    borderWidth: 1,
    borderRadius: 16,
    padding: 16,
    gap: 12,
  },
  point: {
    color: colors.textSecondary,
    fontSize: 14,
    lineHeight: 20,
  },
});


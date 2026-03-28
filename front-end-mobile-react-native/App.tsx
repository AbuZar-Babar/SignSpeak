import React, { useMemo, useState } from "react";
import { SafeAreaView, StyleSheet, Text, Pressable, View } from "react-native";
import { StatusBar } from "expo-status-bar";
import { colors } from "./src/theme/colors";
import { TranslateScreen } from "./src/screens/TranslateScreen";
import { PlaceholderScreen } from "./src/screens/PlaceholderScreen";

type TabKey = "translate" | "dictionary" | "profile" | "complaints";

interface TabConfig {
  key: TabKey;
  label: string;
}

const tabs: TabConfig[] = [
  { key: "translate", label: "Translate" },
  { key: "dictionary", label: "Dictionary" },
  { key: "profile", label: "Profile" },
  { key: "complaints", label: "Complaints" },
];

export default function App() {
  const [activeTab, setActiveTab] = useState<TabKey>("translate");

  const content = useMemo(() => {
    if (activeTab === "translate") {
      return <TranslateScreen />;
    }

    if (activeTab === "dictionary") {
      return (
        <PlaceholderScreen
          title="PSL Dictionary"
          points={[
            "Search and browse PSL words",
            "Show reference animations and examples",
            "Bookmark difficult words for practice",
          ]}
        />
      );
    }

    if (activeTab === "profile") {
      return (
        <PlaceholderScreen
          title="Profile"
          points={[
            "Account details and language preferences",
            "Saved sessions and translation history",
            "Accessibility settings",
          ]}
        />
      );
    }

    return (
      <PlaceholderScreen
        title="Complaints"
        points={[
          "Submit wrong prediction reports",
          "Attach sample clip and expected label",
          "Track complaint status",
        ]}
      />
    );
  }, [activeTab]);

  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar style="light" />
      <View style={styles.header}>
        <Text style={styles.appTitle}>SignSpeak</Text>
        <Text style={styles.appSubtitle}>React Native Translation MVP</Text>
      </View>

      <View style={styles.body}>{content}</View>

      <View style={styles.tabBar}>
        {tabs.map((tab) => {
          const active = tab.key === activeTab;
          return (
            <Pressable
              key={tab.key}
              style={[styles.tabButton, active ? styles.tabButtonActive : undefined]}
              onPress={() => setActiveTab(tab.key)}
            >
              <Text style={[styles.tabLabel, active ? styles.tabLabelActive : undefined]}>
                {tab.label}
              </Text>
            </Pressable>
          );
        })}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.appBg,
  },
  header: {
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 10,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
    backgroundColor: "#0a1b2a",
  },
  appTitle: {
    color: colors.textPrimary,
    fontSize: 28,
    fontWeight: "900",
    letterSpacing: 0.6,
  },
  appSubtitle: {
    color: colors.textSecondary,
    fontSize: 12,
    fontWeight: "700",
    letterSpacing: 0.5,
    marginTop: 2,
  },
  body: {
    flex: 1,
  },
  tabBar: {
    borderTopColor: colors.border,
    borderTopWidth: 1,
    flexDirection: "row",
    paddingHorizontal: 8,
    paddingVertical: 8,
    gap: 8,
    backgroundColor: "#0a1b2a",
  },
  tabButton: {
    flex: 1,
    minHeight: 44,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 12,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: colors.cardBg,
  },
  tabButtonActive: {
    backgroundColor: "rgba(32, 201, 151, 0.18)",
    borderColor: colors.accentMain,
  },
  tabLabel: {
    color: colors.textSecondary,
    fontWeight: "700",
    fontSize: 12,
  },
  tabLabelActive: {
    color: colors.accentMain,
    fontWeight: "900",
  },
});


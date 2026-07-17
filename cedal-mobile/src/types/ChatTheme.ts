export type ChatThemeKind = "none" | "userPhoto" | "preset";

export type ChatTheme = {
  kind: ChatThemeKind;
  // background
  backgroundImageUrl?: string | null;
  backgroundColor?: string | null;

  // text decoration
  textColor?: string | null;
  textFontFamily?: string | null;
  textSize?: number | null;

  // bubble decoration
  bubbleMeColor?: string | null;
  bubbleOtherColor?: string | null;

  // tab decoration
  tabAccentColor?: string | null;
  tabLabelColor?: string | null;

  // for preset lookup
  presetId?: string | null;
};

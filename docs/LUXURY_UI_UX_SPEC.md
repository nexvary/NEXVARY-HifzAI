# NEXVARY HifzAI — Luxury UI/UX Design System 1.0

## Design intent
**مصحف ملكي، هادئ، شديد الوضوح، بلا ازدحام بصري.** Primary need: reading correctly and memorizing easily. Ornamentation is secondary.

### Colors
| Role | Color | Rule |
|---|---|---|
| Royal night background | `#081521` | Main scaffold and empty areas |
| Elevated surface | `#122B39` | Cards / bottom navigation |
| Deep slate | `#1B3441` | Secondary controls |
| Muted metallic gold | `#D6B465` | Selected states / headings, never canonical ayah emphasis replacing orthography |
| Pale silver text | `#EEF1F2` | Standard text |
| Secondary text | `#B9C6CF` | Explanations |
| Emerald progress | `#43C8AA` | Readiness indicators, not religious verdict |
| Warning accent | `#B04D52` | Errors and *unapproved* watermark only |

### Layout specifications
- Native RTL for Arabic, ensure Latin domain/path snippets are LTR inside their own components.
- Bottom navigation maximum 5 primary areas: **الرئيسية، احفظ، المصحف، تقدمي، المزيد**. The teachers and reciters appear as distinct secondary tabs under **المزيد** or structured pages; never overflow the bottom navigation.
- Onboarding first run: حدد هدفك → اختر القارئ/أسلوب التمرين → ابدأ جلسة قصيرة. Skippable; no registration required for local-only features.
- **Home:** at most 3 meaningful metrics (due reviews, today's focused task, self-rated performance), a single primary CTA **ابدأ الحفظ**.
- **Mushaf reader:** distraction-free verse surface, source/riwayah identity, verse-number badges; quiet support for full-screen, adjustable text size, paging and selection.
- **Practice:** one verse/segment and one target action at a time. 5 clear stages: قراءة → إخفاء 35% → إخفاء 70% → تسميع من الذاكرة → ربط الآيات. Progress marker of stage 1/5. Buttons that need explanation have visible labels.
- **Recitation:** authorized player with repeat and speed controls; non-licensed files never appear as playable buttons.
- **Insights:** highlight *self-reported* evidence vs independently checked recitation as separate data categories.
- **Teachers:** only verified educators and clear safeguards for minors, with strong moderation and privacy controls.

### Typography and Quran integrity
- Quran text font must be explicitly reviewed for Uthmani marks on every relevant Android device; **never alter canonical text or replace characters to accommodate typography**.
- Arabic UI font must render cleanly and use a legally reusable font with attribution if necessary. If no suitable verified external font is available, use OS system fonts for non-Quran UI until typography is validated.
- Quran text minimum user-adjustable display area; preferred default 28sp+ depending on screen and font; proper diacritic clearance and line-height; user controls scaling and contrast.
- Arabic controls >= 16sp where practical; touch targets >= 48dp, clear focused/pressed states, accessible labels and content descriptions.
- Shapes: 16–22dp corner radius, 1dp border or subtle shadow on cards; **avoid gold outlines on every single element**.
- Real vector icons (Android Compose material vectors / licensed SVG), **no Emoji icons** or decorative fake controls.
- One task per screen; avoid full-height nested scroll controls that block accessibility; always visible in-app back where applicable.
- Tablets: two-column detail/list layouts or padded max-width reading panel; 320dp mobile view must not clip Arabic/diacritics.

### Mandatory screens before the single committee submission
1. Welcome / goal selection / optional offline setup.
2. Home and adaptive daily plan.
3. Full vetted Mushaf reader + navigation.
4. Recitation and authorized reciter profiles Egypt/Saudi.
5. Memorization ladder with progressive masking and reveal tracking.
6. Similar-verse exercises and verse-bridge practice.
7. Audio recitation practice with explicit limits to automatic judgment.
8. Memory evidence and revision calendar.
9. Teacher selection / sessions / role and parental controls, if academy is within this release's feature contract.
10. Settings: font, theme, privacy, account/data deletion, sources/licenses and in-app update.
11. All loading/error/empty/offline states.
12. About, contact, build version, rights and *unapproved* state until review.

### Proof and acceptance
- Make a **real screenshot pack from an APK in emulator/device**; concept renders are not screenshots of working functionality.
- At least a small Android device, a typical phone, a large phone, a tablet, dark/light and larger font settings where implemented.
- No clipped Arabic, overlapping icons, inaccessible actions, tiny tap targets or dead buttons.
- UX test with novices and experienced memorizers: reach a first memorization task without guidance, understand hints vs independent recall, safely find source/riwayah and recovery path.
- No green "approved" badges or fabricated scholarly credentials; visual status clearly displays whether a feature is a demonstration, verified implementation or expert-approved.

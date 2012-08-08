package loop;

/**************************************************************
 *
 *  The three exported functions are:
 *
 *   1. unicode_charclass
 *
 *      Return new copy of argument with these 14 escapes:
 *
 *        --   \s \S       \v \V       \h \H
 *        --   \w \W       \b \B       \d \D
 *        --   \X          \R
 *
 *      converted into equivalents that work with Unicode.
 *
 *   2. unescape_perl_string
 *
 *      Returns new copy of argument with these string
 *      backslash escapes replaced with the real characters:
 *
 *      --  \a \e \f \n \r \t [but not \b due to previous function]
 *      --  \cX (on ASCII only)
 *      --  \0 \0N \0NN \N \NN \NNN [with o{} on TODO list]
 *      --  \xXX   (X=2) \x{XXXXXX} (X = 1-8)
 *      --  \[IDIOT JAVA PREPROCESSOR]uXXXX (X=4) \UXXXXXXXX (X=8)
 *
 *       NB: \x{...} and \U are *logical* Unicode code points,
 *           not lame-o multiword UTF-16 physical char actors!!!
 *
 *      Used for expanding \n \t \x{..} etc in strings read
 *      in from files with embedded escapes.
 *
 *     XXX: must rewrite to merge 1 and 2 internally, then provide
 *          different API to get at either or both
 *
 *   3. uniplus
 *
 *      Returns its argument rendered into what is essentially
 *      Perl's "U+%v02X" notation.
 *
 *  There are also various exported string constants for a few other
 *  things, like better edges and natural-language words instead of
 *  identifier words.
 *
 *      Tom Christiansen <tchrist@perl.com>
 *      Sun Nov 28 12:55:24 MST 2010
 *
 *      Tue Nov 30 07:47:45 MST 2010
 *      Added extended grapheme cluster -- almost.
 **************************************************************/
public class Escaper {

  /*
  * Because Java's \w and \W are unusable.
  *
  * Note that here and elsewhere in this file, the word "word"
  * means any alpha-num-under character--that is, a program
  * identifierifier.  It is unrelated to natural-language words.
  *
  * For those, look at the natural_word_chars
  */

  private final static String
      identifier_chars = "\\pL"          /* all Letters      */
      + "\\pM"          /* all Marks        */
      + "\\p{Nd}"       /* Decimal Number   */
      + "\\p{Nl}"       /* Letter Number    */
      + "\\p{Pc}"       /* Connector Punctuation           */
      + "["             /*    or else chars which are both */
      +     "\\p{InEnclosedAlphanumerics}"
      +   "&&"          /*    and also      */
      +     "\\p{So}"   /* Other Symbol     */
      + "]";

  public final static String
      identifier_charclass     = "["  + identifier_chars + "]";       /* \w */

  public final static String
      not_identifier_charclass = "[^" + identifier_chars + "]";       /* \W */

  /*
  * Because Java's \b is unusable.
  *
  * If only \b worked, we could have just one boundary.
  *
  * And if conditionals worked, we could have just two:
  *
  *     boundary_before      is  (?(?=\w)(?<!\w)|(?<=\w))
  *     boundary_after       is  (?(?<=\w)(?!\w)|(?=\w))
  *
  * But this is Java, so they don't, which means we need four:
  *
  *  boundary_before_word     is  (?<!\w)
  *  boundary_before_not_word is  (?<=\w)
  *  boundary_after_word      is  (?!\w)
  *  boundary_after_not_word  is  (?=\w)
  *
  * Because Java's \B is unusable.
  *
  * If only \B worked, we could have just one not_boundary.
  *
  * And if conditionals worked, we could have just two:
  *
  *      not_boundary_after       is  (?(?<=\w)(?=\w)|(?!\w))
  *      not_boundary_before      is  (?(?=\w)(?<=\w)|(?<!\w))
  *
  * But this is Java, so they don't, which means we need four:
  *
  *      not_boundary_before_word      is  (?<=\w)
  *      not_boundary_before_not_word  is  (?<!\w)
  *      not_boundary_after_word       is  (?=\w)
  *      not_boundary_after_not_word   is  (?!\w)
  *
  */

  private final static String
      boundary_after_not_word      = "(?="  + identifier_charclass + ")";

  private final static String
      not_boundary_after_word      = boundary_after_not_word;

  public final static String
      precedes_word                = boundary_after_not_word;

  private final static String
      boundary_after_word          = "(?!"  + identifier_charclass + ")";

  private final static String
      not_boundary_after_not_word  = boundary_after_word;

  public final static String
      not_precedes_word            = boundary_after_word;

  private final static String
      boundary_before_not_word     = "(?<=" + identifier_charclass + ")";

  private final static String
      not_boundary_before_word     = boundary_before_not_word;

  public final static String
      follows_word                 = boundary_before_not_word;

  private final static String
      boundary_before_word         = "(?<!" + identifier_charclass + ")";

  private final static String
      not_boundary_before_not_word = boundary_before_word;

  public final static String
      not_follows_word             = boundary_before_word;

  /*
  * a \b is the same as (?:(?<=\w)(?!\w)|(?<!\w)(?=\w))
  *
  */
  public final static String
      boundary        = "(?:"                                         /* \b */
      // IF
      +       follows_word
      // THEN
      +       not_precedes_word
      +   "|"  // ELSE
      // IF
      +       not_follows_word
      // THEN
      +       precedes_word
      +  ")"
      ;

  /*
  * a \B is the same as (?:(?<=\w)(?=\w)|(?<!\w)(?!\w))
  */
  public final static String
      not_boundary    = "(?:"                                         /* \B */
      // IF
      +       follows_word
      // THEN
      +       precedes_word
      +   "|"  // ELSE
      // IF
      +       not_follows_word
      // THEN
      +       not_precedes_word
      +  ")"
      ;

  /*
  * Because Java's \s and \S and \p{Space} are all unusable.
  */
  private final static String
      whitespace_chars =  ""       /* dummy empty string for homogeneity */
      + "\\u000A" // LINE FEED (LF)
      + "\\u000B" // LINE TABULATION
      + "\\u000C" // FORM FEED (FF)
      + "\\u000D" // CARRIAGE RETURN (CR)
      + "\\u0020" // SPACE
      + "\\u0085" // NEXT LINE (NEL)
      + "\\u00A0" // NO-BREAK SPACE
      + "\\u1680" // OGHAM SPACE MARK
      + "\\u180E" // MONGOLIAN VOWEL SEPARATOR
      + "\\u2000" // EN QUAD
      + "\\u2001" // EM QUAD
      + "\\u2002" // EN SPACE
      + "\\u2003" // EM SPACE
      + "\\u2004" // THREE-PER-EM SPACE
      + "\\u2005" // FOUR-PER-EM SPACE
      + "\\u2006" // SIX-PER-EM SPACE
      + "\\u2007" // FIGURE SPACE
      + "\\u2008" // PUNCTUATION SPACE
      + "\\u2009" // THIN SPACE
      + "\\u200A" // HAIR SPACE
      + "\\u2028" // LINE SEPARATOR
      + "\\u2029" // PARAGRAPH SEPARATOR
      + "\\u202F" // NARROW NO-BREAK SPACE
      + "\\u205F" // MEDIUM MATHEMATICAL SPACE
      + "\\u3000" // IDEOGRAPHIC SPACE
      ;

  public final static String
      whitespace_charclass  =                           /* \s */
      "["  + whitespace_chars + "]";

  public final static String
      not_whitespace_charclass =                        /* \S */
      "[^" + whitespace_chars + "]";

  /*
  * this is to avoid variable length lookbehind
  */
  public final static String               /********************/
      space_edge_left = "(?:"                  /* an "improved" \b */
      +     "(?<=^)"    /* to the left      */
      +   "|"           /********************/
      +     "(?<="
      +           whitespace_charclass
      +     ")"
      +  ")";

  public final static String                   /********************/
      space_edge_right = "(?="                     /* an "improved" \b */
      +       "$"          /* to the right     */
      +  "|"               /********************/
      +        whitespace_charclass
      + ")";

  /*
  * Because Java's \p{Alpha} is unusably ASCII-only.
  */
  private final static String
      alphabetic_chars = "\\pL"                   /* all Letters    */
      + "\\pM"            /* all Marks      */
      + "\\p{Nl}"         /* Letter Number  */
      ;

  public final static String
      alphabetic_charclass     = "["  + alphabetic_chars + "]"; /* \p{Alpha} */

  public final static String
      not_alphabetic_charclass = "[^" + alphabetic_chars + "]"; /* \P{Alpha} */

  /*
  * Because Java's \d is ASCII-only.
  */
  public final static String
      digits_charclass     = "\\p{Nd}";  /* \d */

  public final static String
      not_digits_charclass = "\\P{Nd}";  /* \D */


  /*
  * Because Java's \p{Hyphen} is missing.
  */
  private final static String
      hyphen_chars = ""        /* dummy empty string for homogeneity */
      + "\\u002D" // HYPHEN-MINUS
      + "\\u00AD" // SOFT HYPHEN
      + "\\u058A" // ARMENIAN HYPHEN
      + "\\u1806" // MONGOLIAN TODO SOFT HYPHEN
      + "\\u2010" // HYPHEN
      + "\\u2011" // NON-BREAKING HYPHEN
      + "\\u2E17" // DOUBLE OBLIQUE HYPHEN
      + "\\u30FB" // KATAKANA MIDDLE DOT
      + "\\uFE63" // SMALL HYPHEN-MINUS
      + "\\uFF0D" // FULLWIDTH HYPHEN-MINUS
      + "\\uFF65" // HALFWIDTH KATAKANA MIDDLE DOT
      ;

  public final static String
      hyphen_charclass = "["  + hyphen_chars + "]"; /* \p{Hyphen} */

  public final static String
      not_hyphen_charclass = "[^" + hyphen_chars + "]"; /* \P{Hyphen} */

  /*
  * Because Java's \p{Dash} is missing,
  * and \p{Pd} is missing important
  * things like MINUS SIGN.
  */

  private final static String
      dash_chars     =  ""        /* dummy empty string for homogeneity */
      +  "\\u002D" // HYPHEN-MINUS
      +  "\\u058A" // ARMENIAN HYPHEN
      +  "\\u05BE" // HEBREW PUNCTUATION MAQAF
      +  "\\u1400" // CANADIAN SYLLABICS HYPHEN
      +  "\\u1806" // MONGOLIAN TODO SOFT HYPHEN
      +  "\\u2010" // HYPHEN
      +  "\\u2011" // NON-BREAKING HYPHEN
      +  "\\u2012" // FIGURE DASH
      +  "\\u2013" // EN DASH
      +  "\\u2014" // EM DASH
      +  "\\u2015" // HORIZONTAL BAR
      +  "\\u2053" // SWUNG DASH
      +  "\\u207B" // SUPERSCRIPT MINUS
      +  "\\u208B" // SUBSCRIPT MINUS
      +  "\\u2212" // MINUS SIGN
      +  "\\u2E17" // DOUBLE OBLIQUE HYPHEN
      +  "\\u2E1A" // HYPHEN WITH DIAERESIS
      +  "\\u301C" // WAVE DASH
      +  "\\u3030" // WAVY DASH
      +  "\\u30A0" // KATAKANA-HIRAGANA DOUBLE HYPHEN
      +  "\\uFE31" // PRESENTATION FORM FOR VERTICAL EM DASH
      +  "\\uFE32" // PRESENTATION FORM FOR VERTICAL EN DASH
      +  "\\uFE58" // SMALL EM DASH
      +  "\\uFE63" // SMALL HYPHEN-MINUS
      +  "\\uFF0D" // FULLWIDTH HYPHEN-MINUS
      ;

  public final static String
      dash_charclass     = "["  + dash_chars + "]"; /* \p{Dash} */

  public final static String
      not_dash_charclass = "[^" + dash_chars + "]"; /* \P{Dash} */

  /*
  * Because Java's \p{QMark} is missing.
  */

  private final static String
      quotation_mark_chars = ""    /* dummy empty string for homogeneity */
      +  "\\u0022"   // QUOTATION MARK
      +  "\\u0027"   // APOSTROPHE
      +  "\\u00AB"   // LEFT-POINTING DOUBLE ANGLE QUOTATION MARK
      +  "\\u00BB"   // RIGHT-POINTING DOUBLE ANGLE QUOTATION MARK
      +  "\\u2018"   // LEFT SINGLE QUOTATION MARK
      +  "\\u2019"   // RIGHT SINGLE QUOTATION MARK
      +  "\\u201A"   // SINGLE LOW-9 QUOTATION MARK
      +  "\\u201B"   // SINGLE HIGH-REVERSED-9 QUOTATION MARK
      +  "\\u201C"   // LEFT DOUBLE QUOTATION MARK
      +  "\\u201D"   // RIGHT DOUBLE QUOTATION MARK
      +  "\\u201E"   // DOUBLE LOW-9 QUOTATION MARK
      +  "\\u201F"   // DOUBLE HIGH-REVERSED-9 QUOTATION MARK
      +  "\\u2039"   // SINGLE LEFT-POINTING ANGLE QUOTATION MARK
      +  "\\u203A"   // SINGLE RIGHT-POINTING ANGLE QUOTATION MARK
      +  "\\u300C"   // LEFT CORNER BRACKET
      +  "\\u300D"   // RIGHT CORNER BRACKET
      +  "\\u300E"   // LEFT WHITE CORNER BRACKET
      +  "\\u300F"   // RIGHT WHITE CORNER BRACKET
      +  "\\u301D"   // REVERSED DOUBLE PRIME QUOTATION MARK
      +  "\\u301E"   // DOUBLE PRIME QUOTATION MARK
      +  "\\u301F"   // LOW DOUBLE PRIME QUOTATION MARK
      +  "\\uFE41"   // PRESENTATION FORM FOR VERTICAL LEFT CORNER BRACKET
      +  "\\uFE42"   // PRESENTATION FORM FOR VERTICAL RIGHT CORNER BRACKET
      +  "\\uFE43"   // PRESENTATION FORM FOR VERTICAL LEFT WHITE CORNER BRACKET
      +  "\\uFE44"   // PRESENTATION FORM FOR VERTICAL RIGHT WHITE CORNER BRACKET
      +  "\\uFF02"   // FULLWIDTH QUOTATION MARK
      +  "\\uFF07"   // FULLWIDTH APOSTROPHE
      +  "\\uFF62"   // HALFWIDTH LEFT CORNER BRACKET
      +  "\\uFF63"   // HALFWIDTH RIGHT CORNER BRACKET
      ;

  public final static String
      quotation_mark_charclass     =            /* \p{Quotation_Mark} */
      "["  + quotation_mark_chars + "]";

  public final static String
      not_quotation_mark_charclass =            /* \P{Quotation_Mark} */
      "[^" + quotation_mark_chars + "]";

  private final static String
      apostrophic_chars =  ""        /* dummy empty string for homogeneity */
      +  "\\u0027"   // APOSTROPHE
      +  "\\u02BC"   // MODIFIER LETTER APOSTROPHE
      +  "\\u2019"   // RIGHT SINGLE QUOTATION MARK
      ;

  public final static String
      apostrophic_charclass     =  "["  + apostrophic_chars + "]";

  public final static String
      not_apostrophic_charclass =  "[^" + apostrophic_chars + "]";

  private final static String
      natural_word_chars = alphabetic_chars
      + apostrophic_chars
      + dash_chars;

  public final static String
      natural_word_charclass     =  "["  + natural_word_chars + "]";

  public final static String
      not_natural_word_charclass =  "[^" + natural_word_chars + "]";

  private final static String
      vertical_whitespace_chars = ""   /* \v */
      + "\\u000A"     // LINE FEED (LF)
      + "\\u000B"     // LINE TABULATION
      + "\\u000C"     // FORM FEED (FF)
      + "\\u000D"     // CARRIAGE RETURN (CR)
      + "\\u0085"     // NEXT LINE (NEL)
      + "\\u2028"     // LINE SEPARATOR
      + "\\u2029"     // PARAGRAPH SEPARATOR
      ;

  public final static String
      vertical_whitespace_charclass     = "["   + vertical_whitespace_chars + "]";

  public final static String
      not_vertical_whitespace_charclass = "[^"  + vertical_whitespace_chars + "]";

  private final static String
      horizontal_whitespace_chars = ""
      + "\\u0009"    // CHARACTER TABULATION
      + "\\u0020"    // SPACE
      + "\\u00A0"    // NO-BREAK SPACE
      + "\\u1680"    // OGHAM SPACE MARK
      + "\\u180E"    // MONGOLIAN VOWEL SEPARATOR
      + "\\u2000"    // EN QUAD
      + "\\u2001"    // EM QUAD
      + "\\u2002"    // EN SPACE
      + "\\u2003"    // EM SPACE
      + "\\u2004"    // THREE-PER-EM SPACE
      + "\\u2005"    // FOUR-PER-EM SPACE
      + "\\u2006"    // SIX-PER-EM SPACE
      + "\\u2007"    // FIGURE SPACE
      + "\\u2008"    // PUNCTUATION SPACE
      + "\\u2009"    // THIN SPACE
      + "\\u200A"    // HAIR SPACE
      + "\\u202F"    // NARROW NO-BREAK SPACE
      + "\\u205F"    // MEDIUM MATHEMATICAL SPACE
      + "\\u3000"    // IDEOGRAPHIC SPACE
      ;

  public final static String
      horizontal_whitespace_charclass =            /* \h */
      "["   + horizontal_whitespace_chars + "]";

  public final static String
      not_horizontal_whitespace_charclass =        /* \H */
      "[^"  + horizontal_whitespace_chars + "]";

  public final static String
      linebreak = "(?:"                            /* \R */
      +      "(?>\\u000D\\u000A)"
      +   "|"
      +      vertical_whitespace_charclass
      + ")";

  public final static String
      legacy_grapheme_cluster = "(?>\\PM\\pM*)";   /* old \X */

  /*
  * Extended Grapheme Cluster rules from
  *      http://www.unicode.org/reports/tr29/#Default_Grapheme_Cluster_Table
  *
  *  EGC = ( CR LF )
  *    | ( Prepend*
  *        ( L+ | (L* ( ( V | LV ) V* | LVT ) T*) | T+ | [^ Control CR LF ] )
  *        ( Extend | SpacingMark )*
  *       )
  *    | .
  *
  */

  private final static String
      GCB_CR = "\\u000D";         // CARRIAGE RETURN (CR)

  private final static String
      GCB_LF = "\\u000A";         // LINE FEED (LF)

  private final static String
      GCB_CRLF = GCB_CR + GCB_LF;

  /*
  * % unichars -ua '[\p{Zl}\p{Zp}\p{Cc}\p{Cf}]' '[^\x{000D}\x{000A}\x{200C}\x{200D}]' | wc -l
  * 203
  */
  private final static String
      GCB_Control = "["
      + "\\p{Zl}"             // Line Separator
      + "\\p{Zp}"             // Paragraph Separator
      + "\\p{Cc}"             // Control
      + "\\p{Cf}"             // Format
      + "&&[^"                //    and not
      +       "\\u000D"       // CARRIAGE RETURN (CR)
      +       "\\u000A"       // LINE FEED (LF)
      +       "\\u200C"       // ZERO WIDTH NON-JOINER
      +       "\\u200D"       // ZERO WIDTH JOINER
      + "]]";

  /*
  * % unichars -u '\p{Grapheme_Extend = true}'|wc -l
  *    925
  */
  private final static String
      GCB_Extend = "["
      + "\\p{Mn}"      // Nonspacing_Mark
      + "\\p{Me}"      // Enclosing_Mark
      + "\\u200C"     // ZERO WIDTH NON-JOINER
      + "\\u200D"     // ZERO WIDTH JOINER
      // plus a few Spacing_Marks needed for canonical equivalence.
      + "\\u0488"     // COMBINING CYRILLIC HUNDRED THOUSANDS SIGN
      + "\\u0489"     // COMBINING CYRILLIC MILLIONS SIGN
      + "\\u20DD"     // COMBINING ENCLOSING CIRCLE
      + "\\u20DE"     // COMBINING ENCLOSING SQUARE
      + "\\u20DF"     // COMBINING ENCLOSING DIAMOND
      + "\\u20E0"     // COMBINING ENCLOSING CIRCLE BACKSLASH
      + "\\u20E2"     // COMBINING ENCLOSING SCREEN
      + "\\u20E3"     // COMBINING ENCLOSING KEYCAP
      + "\\u20E4"     // COMBINING ENCLOSING UPWARD POINTING TRIANGLE
      + "\\uA670"     // COMBINING CYRILLIC TEN MILLIONS SIGN
      + "\\uA671"     // COMBINING CYRILLIC HUNDRED MILLIONS SIGN
      + "\\uA672"     // COMBINING CYRILLIC THOUSAND MILLIONS SIGN
      + "\\uFF9E"     // HALFWIDTH KATAKANA VOICED SOUND MARK
      + "\\uFF9F"     // HALFWIDTH KATAKANA SEMI-VOICED SOUND MARK
      + "]";

  private final static String
      GCB_Prepend = "["
      + "\\u0E40"     // THAI CHARACTER SARA E
      + "\\u0E41"     // THAI CHARACTER SARA AE
      + "\\u0E42"     // THAI CHARACTER SARA O
      + "\\u0E43"     // THAI CHARACTER SARA AI MAIMUAN
      + "\\u0E44"     // THAI CHARACTER SARA AI MAIMALAI
      + "\\u0EC0"     // LAO VOWEL SIGN E
      + "\\u0EC1"     // LAO VOWEL SIGN EI
      + "\\u0EC2"     // LAO VOWEL SIGN O
      + "\\u0EC3"     // LAO VOWEL SIGN AY
      + "\\u0EC4"     // LAO VOWEL SIGN AI
      + "\\uAAB5"     // TAI VIET VOWEL E
      + "\\uAAB6"     // TAI VIET VOWEL O
      + "\\uAAB9"     // TAI VIET VOWEL UEA
      + "\\uAABB"     // TAI VIET VOWEL AUE
      + "\\uAABC"     // TAI VIET VOWEL AY
      + "]";

  private final static String
      GCB_Spacing_Mark = "["
      + "\\p{Mc}"       // Spacing_Mark
      + "\\u0E30"     // THAI CHARACTER SARA A
      + "\\u0E32"     // THAI CHARACTER SARA AA
      + "\\u0E33"     // THAI CHARACTER SARA AM
      + "\\u0E45"     // THAI CHARACTER LAKKHANGYAO
      + "\\u0EB0"     // LAO VOWEL SIGN A
      + "\\u0EB2"     // LAO VOWEL SIGN AA
      + "\\u0EB3"     // LAO VOWEL SIGN AM
      + ""            // XXX: MISSING!
      /*
      XXX: * too big to enumerate Grapheme_Cluster_Break != Extend
      *  % unichars -au '\p{Mc}' '\P{Grapheme_Cluster_Break=Extend}' | wc -l
      *       268
      */
      + "]";

  /*
  * L        Hangul_Syllable_Type=L, that is:
  *     U+1100 HANGUL CHOSEONG KIYEOK
  *     .. U+115F HANGUL CHOSEONG FILLER
  *     U+A960 HANGUL CHOSEONG TIKEUT-MIEUM
  *     ..U+A97C HANGUL CHOSEONG SSANGYEORINHIEUH
  *
  * % unichars -ua '\p{Hangul_Syllable_Type=L}' | wc -l
  *      125
  */
  private final static String
      GCB_L = "[\\u1100-\\u115F\\uA960-\\uA97C]";

  /*
  * V        Hangul_Syllable_Type=V, that is:
  *     U+1160 HANGUL JUNGSEONG FILLER
  *     ..U+11A2 HANGUL JUNGSEONG SSANGARAEA
  *     U+D7B0 HANGUL JUNGSEONG O-YEO
  *     ..U+D7C6 HANGUL JUNGSEONG ARAEA-E
  *
  * % unichars -ua '\p{Hangul_Syllable_Type=V}' | wc -l
  *       95
  */
  private final static String
      GCB_V = "[\\u1160-\\u11A2\\uD7B0-\\uD7C6]";


  /*
  * T        Hangul_Syllable_Type=T, that is:
  *     U+11A8 HANGUL JONGSEONG KIYEOK
  *     ..U+11F9 HANGUL JONGSEONG YEORINHIEUH
  *     U+D7CB HANGUL JONGSEONG NIEUN-RIEUL
  *     ..U+D7FB HANGUL JONGSEONG PHIEUPH-THIEUTH
  *
  * % unichars -ua '\p{Hangul_Syllable_Type=T}' | wc -l
  *      137
  */
  private final static String
      GCB_T = "[\\u11A8-\\u11F9\\uD7CB-\\uD7FB]";


  /*
  * LV       Hangul_Syllable_Type=LV, that is:
  *   U+AC00 HANGUL SYLLABLE GA
  *   U+AC1C HANGUL SYLLABLE GAE
  *   U+AC38 HANGUL SYLLABLE GYA
  *   ...
  */
  private final static String
      GCB_LV = "["
      + "\\uAC00"     // HANGUL SYLLABLE GA
      + "\\uAC1C"     // HANGUL SYLLABLE GAE
      + "\\uAC38"     // HANGUL SYLLABLE GYA
      + ""            // XXX: MISSING!
      /*
      *  XXX: missing lots of them
      *  % unichars -ua '\p{Hangul_Syllable_Type=LV}' | wc -l
      *    399
      */
      + "]";

  /*
  * Hangul_Syllable_Type=LVT, that is:
  *     U+AC01 HANGUL SYLLABLE GAG
  *     U+AC02 HANGUL SYLLABLE GAGG
  *     U+AC03 HANGUL SYLLABLE GAGS
  *     U+AC04 HANGUL SYLLABLE GAN
  *     ...
  */
  private final static String
      GCB_LVT = "["
      + "\\uAC01"     // HANGUL SYLLABLE GAG
      + "\\uAC02"     // HANGUL SYLLABLE GAGG
      + "\\uAC03"     // HANGUL SYLLABLE GAGS
      + "\\uAC04"     // HANGUL SYLLABLE GAN
      + ""            // XXX: MISSING!
      /*
      *  XXX: missing a *MYRIAD*
      *  % unichars -ua '\p{Hangul_Syllable_Type=LVT}' | wc -l
      *    10773
      */
      + "]";

  /*
  * WHEW! Now we're ready to build the ECG, which as I'm sure
  *       you have by now forgotten, goes this way:
  *
  *  EGC =   ( CR LF )
  *        | ( Prepend*
  *            ( L+ | (L* ( ( V | LV ) V* | LVT ) T*) | T+ | [^ Control CR LF ] )
  *            ( Extend | SpacingMark )*
  *          )
  *        | .
  *
  *
  *   Which breaks out like this:
  *
  *     # 1          EGC =  (
  *     # 2              ( CR LF )
  *     # 3            | ( Prepend*
  *     # 4                (
  *     # 5                      L+
  *     # 6                  |
  *     # 7                    (
  *     # 8                      L*
  *     # 9                      (
  *     #10                          ( V | LV ) V*
  *     #11                         | LVT
  *     #12                      )
  *     #13                      T*
  *     #14                    )
  *     #15                 | T+
  *     #16                 | [^ Control CR LF ]
  *     #17                )
  *     #18                ( Extend | SpacingMark )*
  *     #19               )
  *     #20            | .
  *     #21          )
  *
  * Which in turn corresponds to this:
  *
  */

  public final static String              /* new \X */
      extended_grapheme_cluster =
      /* #01 */   "(?:"
      /* #02 */ +       "(?:" + GCB_CRLF + ")"
      /* #03 */ +    "|"
      /* #03 */ +       "(?:"
      /* #03 */ +            GCB_Prepend  + "*"
      /* #04 */ +           "(?:"
      /* #05 */ +                  GCB_L + "+"
      /* #06 */ +                 "|"
      /* #07 */ +                   "("
      /* #08 */ +                       GCB_L + "*"
      /* #09 */ +                       "("
      /* #10 */ +                          "(?:[" + GCB_V + GCB_LV + "]"
      /* #10 */ +                                                       GCB_V + "*"
      /* #11 */ +                             "|" + GCB_LVT
      /* #12 */ +                          ")"
      /* #13 */ +                           GCB_T + "*"
      /* #14 */ +                     ")"
      /* #14 */ +                   ")"
      /* #15 */ +                 "|"
      /* #15 */ +                  GCB_T + "+"
      /* #16 */ +              "|"
      /* #16 */ +                  "[^" + GCB_Control + GCB_CRLF + "]"
      /* #17 */ +       ")"
      /* #18 */ +       "[" + GCB_Extend + GCB_Spacing_Mark + "]*"
      /* #19 */ +       ")"
      /* #20 */ +     "|(?s:.)"
      /* #21 */ +  ")"
      ;

  /******************************************************
   * Translate
   *    \w \W \s \S \v \V \h \H \d \D \b \B \X \R
   * into Unicode-correct code.
   ******************************************************/
  public final static String
  unicode_charclass(String oldstr) {

    StringBuffer newstr; {
      /*
      * Collectively these 14 recognized escapes...
      *
      *   \w \W \s \S \v \V \h \H \d \D \b \B \X \R
      *
      * ...go from needing 2 chars each on avg to needing 99.
      * So quickly count up backslashes, adding 100 chars
      * to initial buffer size per backslash encountered.
      *
      * Don't worry about surrogates here.
      */
      int newlen = oldstr.length();
      for (int i = 0; i < oldstr.length(); i++) {
        if (oldstr.charAt(i) == '\\') {
          newlen += 100;
        }
      }
      newstr = new StringBuffer(newlen);
    }

    boolean saw_backslash = false;

    for (int curpos = 0; curpos < oldstr.length(); curpos++) {
      int curchar = oldstr.codePointAt(curpos);

      if (oldstr.codePointAt(curpos) > Character.MAX_VALUE) {
        curpos++; /****WE HATES UTF-16! WE HATES IT FOREVERSES!!!****/
      }

      if (!saw_backslash) {
        if (curchar == '\\') {
          saw_backslash = true;
        } else {
          newstr.append(Character.toChars(curchar));
        }
        continue; /* for */
      }

      if (curchar == '\\') {
        saw_backslash = false;
        newstr.append("\\\\");
        continue; /* for */
      }

      switch (curchar) {

        case 'b':  newstr.append(boundary);
          break; /* switch */
        case 'B':  newstr.append(not_boundary);
          break; /* switch */

        case 'd':  newstr.append(digits_charclass);
          break; /* switch */
        case 'D':  newstr.append(not_digits_charclass);
          break; /* switch */

        case 'h':  newstr.append(horizontal_whitespace_charclass);
          break; /* switch */
        case 'H':  newstr.append(not_horizontal_whitespace_charclass);
          break; /* switch */

        case 'v':  newstr.append(vertical_whitespace_charclass);
          break; /* switch */
        case 'V':  newstr.append(not_vertical_whitespace_charclass);
          break; /* switch */

        case 'R':  newstr.append(linebreak);
          break; /* switch */

        case 's':  newstr.append(whitespace_charclass);
          break; /* switch */
        case 'S':  newstr.append(not_whitespace_charclass);
          break; /* switch */

        case 'w':  newstr.append(identifier_charclass);
          break; /* switch */
        case 'W':  newstr.append(not_identifier_charclass);
          break; /* switch */

        case 'Y':  newstr.append(legacy_grapheme_cluster);
          break; /* switch */

        case 'X':  newstr.append(extended_grapheme_cluster);
          break; /* switch */

        default:   newstr.append('\\');
          newstr.append(Character.toChars(curchar));
          break; /* switch */

      }
      saw_backslash = false;
    }

    if (saw_backslash) {
      /*
      * Huh! An Unbackslashed backslash was the last character.
      * Good luck with getting *that* past the regex compiler!
      */
      newstr.append('\\');
    }

    return newstr.toString();
  }


  /*******************************************************
   *
   * unescape_perl_string()
   *
   *      Tom Christiansen <tchrist@perl.com>
   *      Sun Nov 28 12:55:24 MST 2010
   *
   * It's completely ridiculous that there's no standard
   * unescape_java_string function.  Since I have to do the
   * damn thing myself, i might as well make it halfway useful
   * by supporting things Java was too stupid to consider in
   * strings:
   *
   *   => "?" items  are additions to Java string escapes
   *                 but normal in Java regexes
   *
   *   => "!" items  are also additions to Java regex escapes
   *
   * Standard singletons: ?\a ?\e \f \n \r \t
   *
   *      NB: \b is unsupported as backspace so it can pass-through
   *          to the regex translator untouched; I refuse to make anyone
   *          doublebackslash it as doublebackslashing is a Java idiocy
   *          I desperately wish would die out.  There are plenty of
   *          other ways to write it:
   *
   *              \cH, \12, \012, \x08 \x{8}, \u0008, \U00000008
   *
   * Octal escapes: \0 \0N \0NN \N \NN \NNN
   *    Can range up to !\777 not \377
   *
   *      TODO: add !\o{NNNNN}
   *          last Unicode is 4177777
   *          maxint is 37777777777
   *
   * Control chars: ?\cX
   *      Means: ord(X) ^ ord('@')
   *
   * Old hex escapes: \xXX
   *      unbraced must be 2 xdigits
   *
   * Perl hex escapes: !\x{XXX} braced may be 1-8 xdigits
   *       NB: proper Unicode never needs more than 6, as highest
   *           valid codepoint is 0x10FFFF, not maxint 0xFFFFFFFF
   *
   * Lame Java escape: \[IDIOT JAVA PREPROCESSOR]uXXXX must be
   *                   exactly 4 xdigits;
   *
   *       I can't write XXXX in this comment where it belongs
   *       because the damned Java Preprocessor can't mind its
   *       own business.  Idiots!
   *
   * Lame Python escape: !\UXXXXXXXX must be exactly 8 xdigits
   *
   * TODO: Perl translation escapes: \Q \U \L \E \[IDIOT JAVA PREPROCESSOR]u \l
   *       These are not so important to cover if you're passing the
   *       result to Pattern.compile(), since it handles them for you
   *       further downstream.  Hm, what about \[IDIOT JAVA PREPROCESSOR]u?
   *
   * XXX: remove Python support; interferes with passing \Q \E \U through
   *      to Java Pattern.compile(), which handles those there.
   *
   */

  public final static String
  unescape_perl_string(String oldstr) {

    /*
     * In contrast to fixing Java's broken regex charclasses,
     * this one need be no bigger, as unescaping shrinks the string
     * here where in the other one, it grows it.
     */

    StringBuffer newstr = new StringBuffer(oldstr.length());

    boolean saw_backslash = false;

    for (int curpos = 0; curpos < oldstr.length(); curpos++) {
      int curchar = oldstr.codePointAt(curpos);
      if (oldstr.codePointAt(curpos) > Character.MAX_VALUE) {
        curpos++; /****WE HATES UTF-16! WE HATES IT FOREVERSES!!!****/
      }

      if (!saw_backslash) {
        if (curchar == '\\') {
          saw_backslash = true;
        } else {
          newstr.append(Character.toChars(curchar));
        }
        continue; /* for */
      }

      if (curchar == '\\') {
        saw_backslash = false;
        newstr.append("\\\\");
        continue; /* for */
      }

      switch (curchar) {

        case 'r':  newstr.append('\r');
          break; /* switch */

        case 'n':  newstr.append('\n');
          break; /* switch */

        case 'f':  newstr.append('\f');
          break; /* switch */

        //XXX//   /* PASS a \b THROUGH!! */
        //XXX//   case 'b':  newstr.append("\\b");
        //XXX//              break; /* switch */

        case 'b':  newstr.append('\b');
          break; /* switch */

        case 't':  newstr.append('\t');
          break; /* switch */

        /*
        * Must use numbers for the next two because they
        * are only in the Java regex engine, not the
        * language itself (just like controls).
        */
        case 'a':  newstr.append('\007');
          break; /* switch */

        case 'e':  newstr.append('\033');
          break; /* switch */

        /*
        * A "control" character is what you get when you xor its
        * codepoint with '@'==64.  This only makes sense for ASCII,
        * and may not yield a "control" character after all.
        *
        * Strange but true: "\c{" is ";", "\c}" is "=", etc.
        *
        * XXX: Must change to match Java, which allows for \c
        *      in front of code point E9 (ACUTE) to create an
        *      A9 (COPYRIGHT SYMBOL).  ^Dummies!
        */
        case 'c':   {
          if (++curpos == oldstr.length()) { die("trailing \\c"); }
          curchar = oldstr.codePointAt(curpos);
          /*
          * don't need to grok surrogates, as next line blows them up
          */
          if (curchar > 0x7F) { die("expected ASCII after \\c"); }
          newstr.append(Character.toChars(curchar ^ 64));
          break; /* switch */
        }

        case '8':
        case '9': die("illegal octal digit");
          /* NOTREACHED */

          /*
          * may be 0 to 2 octal digits following this one
          * so back up one for fallthrough to next case;
          * unread this digit and fall through to next case.
          */
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7': --curpos;
          /* FALLTHROUGH */

          /*
          * Can have 0, 1, or 2 octal digits following a 0
          * this permits larger values than octal 377, up to
          * octal 777.
          */
        case '0': {
          if (curpos+1 == oldstr.length()) {
            /* found \0 at end of string */
            newstr.append(Character.toChars(0));
            break; /* switch */
          }
          curpos++;
          int digits = 0;
          int j;
          for (j = 0; j <= 2; j++) {
            if (curpos+j == oldstr.length()) {
              break; /* for */
            }
            /* safe because will unread surrogate */
            int ch = oldstr.charAt(curpos+j);
            if (ch < '0' || ch > '7') {
              break; /* for */
            }
            digits++;
          }
          if (digits == 0) {
            --curpos;
            newstr.append('\0');
            break; /* switch */
          }
          int value = 0;
          try {
            value = Integer.parseInt(
                oldstr.substring(curpos, curpos+digits), 8);
          } catch (NumberFormatException nfe) {
            die("invalid octal value for \\0 escape");
          }
          newstr.append(Character.toChars(value));
          curpos += digits-1;
          break; /* switch */
        } /* end case '0' */

        case 'x':  {
          if (curpos+2 > oldstr.length()) {
            die("string too short for \\x escape");
          }
          curpos++;
          boolean saw_brace = false;
          if (oldstr.charAt(curpos) == '{') {
            /* ^^^^^^ ok to ignore surrogates here */
            curpos++;
            saw_brace = true;
          }
          int j;
          for (j = 0; j < 8; j++) {

            if (!saw_brace && j == 2) {
              break;  /* for */
            }

            /*
            * ASCII test also catches surrogates
            */
            int ch = oldstr.charAt(curpos+j);
            if (ch > 127) {
              die("illegal non-ASCII hex digit in \\x escape");
            }

            if (saw_brace && ch == '}') { break; /* for */ }

            if (! ( (ch >= '0' && ch <= '9')
                ||
                (ch >= 'a' && ch <= 'f')
                ||
                (ch >= 'A' && ch <= 'F')
            )
                )
            {
              die(String.format(
                  "illegal hex digit #%d '%c' in \\x", ch, ch));
            }

          }
          if (j == 0) { die("empty braces in \\x{} escape"); }
          int value = 0;
          try {
            value = Integer.parseInt(oldstr.substring(curpos, curpos+j), 16);
          } catch (NumberFormatException nfe) {
            die("invalid hex value for \\x escape");
          }
          newstr.append(Character.toChars(value));
          if (saw_brace) { j++; }
          curpos += j-1;
          break; /* switch */
        }

        case 'u': {
          if (curpos+4 > oldstr.length()) {
            die("string too short for \\u escape");
          }
          curpos++;
          int j;
          for (j = 0; j < 4; j++) {
            /* this also handles the surrogate issue */
            if (oldstr.charAt(curpos+j) > 127) {
              die("illegal non-ASCII hex digit in \\u escape");
            }
          }
          int value = 0;
          try {
            value = Integer.parseInt(oldstr.substring(curpos, curpos+j), 16);
          } catch (NumberFormatException nfe) {
            die("invalid hex value for \\u escape");
          }
          newstr.append(Character.toChars(value));
          curpos += j-1;
          break; /* switch */
        }

        /* XXX: this needs to die */
        case 'U': {
          if (curpos+8 > oldstr.length()) {
            die("string too short for \\U escape");
          }
          curpos++;
          int j;
          for (j = 0; j < 8; j++) {
            /* this also handles the surrogate issue */
            if (oldstr.charAt(curpos+j) > 127) {
              die("illegal non-ASCII hex digit in \\U escape");
            }
          }
          int value = 0;
          try {
            value = Integer.parseInt(oldstr.substring(curpos, curpos+j), 16);
          } catch (NumberFormatException nfe) {
            die("invalid hex value for \\U escape");
          }
          newstr.append(Character.toChars(value));
          curpos += j-1;
          break; /* switch */
        }

        default:   newstr.append('\\');
          newstr.append(Character.toChars(curchar));
          /*
          * say(String.format(
          *       "DEFAULT unrecognized escape %c passed through",
          *       curchar));
          */
          break; /* switch, just in case */

      }
      saw_backslash = false;
    }

    /* weird to leave one at the end */
    if (saw_backslash) {
      newstr.append('\\');
    }

    return newstr.toString();
  }

  private static void die(String msg) {
    throw new RuntimeException(msg);
  }

  /********************************************************************
   * Return a string "U+XX.XXX.XXXX" etc, where each XX set is the
   * xdigits of the logical Unicode code point. No bloody brain-damaged
   * UTF-16 surrogate crap, just true logical characters.
   ********************************************************************/
  public final static
  String uniplus(String s) {

    if (s.length() == 0) {
      return "";
    }

    /* This is just the minimum; sb will grow as needed. */
    StringBuffer sb = new StringBuffer(2 + 3 * s.length());
    sb.append("U+");
    for (int i = 0; i < s.length(); i++) {
      /* always at least 2 places so it doesn't look weird */
      sb.append(String.format("%02X", s.codePointAt(i)));
      if (s.codePointAt(i) > Character.MAX_VALUE) {
        i++; /****WE HATES UTF-16! WE HATES IT FOREVERSES!!!****/
      }
      if (i+1 < s.length()) {
        sb.append(".");
      }
    }
    return sb.toString();
  }
}
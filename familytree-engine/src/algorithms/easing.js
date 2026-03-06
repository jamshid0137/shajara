/**
 * familytree-engine/src/algorithms/easing.js
 * Barcha animatsiya easing funksiyalari
 * FamilyTreeJS dagi FamilyTree.anim.* ning to'liq implementatsiyasi
 */

/**
 * Easing funksiyalar to'plami
 * Har bir funksiya [0..1] oralig'idagi t qiymatini qabul qilib,
 * [0..1] oralig'idagi progress qiymatini qaytaradi.
 */
export const Easing = {

  /**
   * Kvadratik kirish (ease-in quad)
   * Sekin boshlanib, tezlashadi
   */
  inPow(t) {
    if (t < 0) return 0;
    if (t > 1) return 1;
    return Math.pow(t, 2);
  },

  /**
   * Kvadratik chiqish (ease-out quad) ← FamilyTreeJS DEFAULT
   * Tez boshlanib, sekinlashadi
   */
  outPow(t) {
    if (t < 0) return 0;
    if (t > 1) return 1;
    return -1 * (Math.pow(t - 1, 2) + -1);
  },

  /**
   * Kvadratik kirish-chiqish (ease-in-out quad)
   */
  inOutPow(t) {
    if (t < 0) return 0;
    if (t > 1) return 1;
    if ((t *= 2) < 1) return Easing.inPow(t) / 2;
    return -0.5 * (Math.pow(t - 2, 2) + -2);
  },

  /**
   * Sinus kirish
   */
  inSin(t) {
    if (t < 0) return 0;
    if (t > 1) return 1;
    return 1 - Math.cos(t * (Math.PI / 2));
  },

  /**
   * Sinus chiqish
   */
  outSin(t) {
    if (t < 0) return 0;
    if (t > 1) return 1;
    return Math.sin(t * (Math.PI / 2));
  },

  /**
   * Sinus kirish-chiqish
   */
  inOutSin(t) {
    if (t < 0) return 0;
    if (t > 1) return 1;
    return -0.5 * (Math.cos(Math.PI * t) - 1);
  },

  /**
   * Eksponent kirish
   */
  inExp(t) {
    if (t < 0) return 0;
    if (t > 1) return 1;
    return Math.pow(2, 10 * (t - 1));
  },

  /**
   * Eksponent chiqish
   */
  outExp(t) {
    if (t < 0) return 0;
    if (t > 1) return 1;
    return 1 - Math.pow(2, -10 * t);
  },

  /**
   * Eksponent kirish-chiqish
   */
  inOutExp(t) {
    if (t < 0) return 0;
    if (t > 1) return 1;
    if (t < 0.5) return 0.5 * Math.pow(2, 10 * (2 * t - 1));
    return 0.5 * (2 - Math.pow(2, 10 * (-2 * t + 1)));
  },

  /**
   * Doira kirish
   */
  inCirc(t) {
    if (t < 0) return 0;
    if (t > 1) return 1;
    return -(Math.sqrt(1 - t * t) - 1);
  },

  /**
   * Doira chiqish
   */
  outCirc(t) {
    if (t < 0) return 0;
    if (t > 1) return 1;
    return Math.sqrt(1 - (t - 1) * (t - 1));
  },

  /**
   * Doira kirish-chiqish
   */
  inOutCirc(t) {
    if (t < 0) return 0;
    if (t > 1) return 1;
    if (t < 1) return -0.5 * (Math.sqrt(1 - t * t) - 1);
    return 0.5 * (Math.sqrt(1 - (2 * t - 2) * (2 * t - 2)) + 1);
  },

  /**
   * Sakrash effekti (bounce out)
   * FamilyTreeJS: FamilyTree.anim.rebound
   */
  rebound(t) {
    if (t < 0) return 0;
    if (t > 1) return 1;
    if (t < 1 / 2.75) return 1 - 7.5625 * t * t;
    if (t < 2 / 2.75) return 1 - (7.5625 * (t - 1.5 / 2.75) * (t - 1.5 / 2.75) + 0.75);
    if (t < 2.5 / 2.75) return 1 - (7.5625 * (t - 2.25 / 2.75) * (t - 2.25 / 2.75) + 0.9375);
    return 1 - (7.5625 * (t - 2.625 / 2.75) * (t - 2.625 / 2.75) + 0.984375);
  },

  /**
   * Orqaga tortish (anticipation)
   */
  inBack(t) {
    if (t < 0) return 0;
    if (t > 1) return 1;
    return t * t * (2.70158 * t - 1.70158);
  },

  /**
   * Orqaga chiqish (overshoot)
   */
  outBack(t) {
    if (t < 0) return 0;
    if (t > 1) return 1;
    return (t - 1) * (t - 1) * (2.70158 * (t - 1) + 1.70158) + 1;
  },

  /**
   * Orqaga kirish-chiqish
   */
  inOutBack(t) {
    if (t < 0) return 0;
    if (t > 1) return 1;
    if (t < 0.5) return 4 * t * t * (7.1898 * t - 2.5949) * 0.5;
    return 0.5 * ((2 * t - 2) * (2 * t - 2) * (3.5949 * (2 * t - 2) + 2.5949) + 2);
  },

  /**
   * Impuls (tez chiqib, sekin tushadi)
   * Formula: t * e^(1-t) * 2
   */
  impulse(t) {
    const k = 2 * t;
    return k * Math.exp(1 - k);
  },

  /**
   * Eksponent puls (gauss shakli)
   */
  expPulse(t) {
    return Math.exp(-2 * Math.pow(t, 2));
  },

  /**
   * Chiziqli (linear)
   */
  linear(t) {
    if (t < 0) return 0;
    if (t > 1) return 1;
    return t;
  },
};

/**
 * Easing funksiyasini nomidan olish
 * @param {string} name - funksiya nomi
 * @returns {Function}
 */
export function getEasingFn(name) {
  return Easing[name] || Easing.outPow;
}

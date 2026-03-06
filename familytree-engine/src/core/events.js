/**
 * familytree-engine/src/core/events.js
 * Voqealar (Pub/Sub) mexanizmi
 */

export class EventEmitter {
  constructor() {
    this.listeners = {};
  }

  /**
   * Hodisaga obuna bo'lish
   * @param {string} event 
   * @param {Function} callback 
   */
  on(event, callback) {
    if (!this.listeners[event]) {
      this.listeners[event] = [];
    }
    this.listeners[event].push(callback);
    return this;
  }

  /**
   * Hodisani bekor qilish
   * @param {string} event 
   * @param {Function} callback 
   */
  off(event, callback) {
    if (!this.listeners[event]) return;
    this.listeners[event] = this.listeners[event].filter(cb => cb !== callback);
  }

  /**
   * Hodisani ishga tushirish (trigger)
   * FamilyTreeJS on('update', function(sender, args) { ... }) shaklida kutadi
   * Return false qilinganda, bekor qilish imkoniyati ham bor.
   */
  emit(event, sender, ...args) {
    if (!this.listeners[event]) return true;
    
    let continueExceution = true;
    for (const callback of this.listeners[event]) {
      const result = callback(sender, ...args);
      if (result === false) {
        continueExceution = false;
      }
    }
    return continueExceution;
  }
}

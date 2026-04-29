/**
 * Simple token-bucket rate limiter to avoid flooding APIs.
 */
export class RateLimiter {
  /**
   * @param {number} maxTokens  Max burst size
   * @param {number} refillMs   Time in ms to refill one token
   */
  constructor(maxTokens = 10, refillMs = 1000) {
    this.maxTokens = maxTokens;
    this.refillMs = refillMs;
    this.tokens = maxTokens;
    this.lastRefill = Date.now();
  }

  _refill() {
    const now = Date.now();
    const elapsed = now - this.lastRefill;
    const newTokens = Math.floor(elapsed / this.refillMs);
    if (newTokens > 0) {
      this.tokens = Math.min(this.maxTokens, this.tokens + newTokens);
      this.lastRefill = now;
    }
  }

  /**
   * Wait until a token is available, then consume it.
   */
  async acquire() {
    this._refill();
    if (this.tokens > 0) {
      this.tokens--;
      return;
    }
    // Wait for next refill
    const waitMs = this.refillMs - (Date.now() - this.lastRefill);
    await new Promise((resolve) => setTimeout(resolve, Math.max(50, waitMs)));
    return this.acquire();
  }
}

/** Global rate limiter for Discord API calls */
export const discordLimiter = new RateLimiter(5, 1000);

/** Global rate limiter for backend API calls */
export const apiLimiter = new RateLimiter(15, 1000);

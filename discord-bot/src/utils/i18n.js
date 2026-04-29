import i18next from 'i18next';
import en from '../locales/en.json' with { type: "json" };
import fr from '../locales/fr.json' with { type: "json" };

i18next.init({
  lng: 'en',
  fallbackLng: 'en',
  resources: {
    en: { translation: en },
    fr: { translation: fr }
  }
});

export function t(key, locale = 'en') {
  // discord locales use format like 'en-US', 'fr'
  const lang = locale.split('-')[0];
  return i18next.t(key, { lng: lang });
}

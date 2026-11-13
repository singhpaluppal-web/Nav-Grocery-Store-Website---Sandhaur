import { Product } from '../types';

export interface ProductSpecification {
  ingredients: string;
  shelfLife: string;
  allergens: string;
  storage: string;
  isNonFood: boolean;
  isFrozen: boolean;
}

const NON_FOOD_PATTERN =
  /\b(surf excel|ariel|tide|rin|comfort|vim|exo|pril|harpic|lizol|colin|dettol|savlon|lifebuoy|dove|pears|lux|santoor|medimix|godrej|colgate|close up|pepsodent|sensodyne|head & shoulders|clinic plus|sunsilk|pantene|parachute|bajaj|amla|nivea|vaseline|ponds|detergent|dishwash|cleaner|antiseptic|soap|toothpaste|shampoo|hair oil|cold cream|body lotion)\b/i;

export function getProductSpecification(product: Product): ProductSpecification {
  const name = (product.name || '').trim();
  const brand = (product.brand || '').trim();
  const hay = `${brand} ${name} ${product.category || ''}`.toLowerCase();
  const isFrozen =
    (product.department || '').toLowerCase().includes('frozen') ||
    (product.category || '').toLowerCase().includes('frozen');
  const isNonFood = NON_FOOD_PATTERN.test(hay);

  if (isNonFood) {
    let ingredients = 'Surfactants, fragrance boosters, conditioning agents, and purified water.';
    let allergens = 'External / household use only. Avoid contact with eyes; rinse with clean water if contact occurs.';
    if (/\b(harpic|lizol|colin|dettol|savlon)\b/i.test(hay)) {
      ingredients = 'Active disinfectant complex, non-ionic surfactants, pine/citrus fragrance, and stabilizers.';
      allergens = 'Do not ingest. Avoid direct skin or eye contact. Keep bottle tightly capped away from children.';
    } else if (/\b(toothpaste|colgate|pepsodent|sensodyne|close up|dabur red)\b/i.test(hay)) {
      ingredients = 'Calcium carbonate / hydrated silica, sorbitol, purified water, sodium fluoride / herbal extracts, mint flavor.';
      allergens = 'Oral care use only. Children under 6 should use a pea-sized amount under supervision.';
    } else if (/\b(soap|dove|pears|lux|santoor|medimix|godrej|lifebuoy)\b/i.test(hay)) {
      ingredients = 'Sodium palmate, sodium palm kernelate, glycerine, natural essential oils, skin moisturizers.';
      allergens = 'External skin use only. Discontinue use if skin irritation develops.';
    } else if (/\b(shampoo|hair oil|nivea|vaseline|ponds|parachute)\b/i.test(hay)) {
      ingredients = 'Purified water, mild cleansing/conditioning agents, nourishing botanical oils, and vitamin E.';
      allergens = 'Contains almond oil (select variants) & fragrance. For external cosmetic use only.';
    }

    return {
      ingredients,
      shelfLife: '24 Months from Date of Manufacture (Sealed Pack)',
      allergens,
      storage: 'Store tightly sealed in a cool, dry place away from direct heat and children.',
      isNonFood: true,
      isFrozen: false,
    };
  }

  if (isFrozen) {
    if (/\bpaneer\b/i.test(hay)) {
      return {
        ingredients: 'Fresh pasteurized milk solids, citric acid / permitted coagulant (100% pure dairy paneer).',
        shelfLife: '9 Months from Packaging (kept continuously at -18°C)',
        allergens: 'CONTAINS MILK / DAIRY (Lactose). Free from added preservatives.',
        storage: 'Keep continuously frozen at -18°C or below. Once thawed, refrigerate and consume within 48 hours.',
        isNonFood: false,
        isFrozen: true,
      };
    }
    if (/\b(fries|tikki|patty|smiles|mccain)\b/i.test(hay)) {
      return {
        ingredients: 'Select graded potatoes, edible refined vegetable oil, potato starch, wheat flour, iodized salt, mixed vegetable solids & mild spices.',
        shelfLife: '12 Months from Packaging (kept continuously at -18°C)',
        allergens: 'CONTAINS WHEAT (GLUTEN). May contain traces of soy and milk solids.',
        storage: 'Store deep frozen at -18°C or below. Fry or air-fry directly from frozen; do not refreeze once thawed.',
        isNonFood: false,
        isFrozen: true,
      };
    }
    if (/\bsoya chaap\b/i.test(hay)) {
      return {
        ingredients: 'Soybean flour chunks, wheat gluten protein, iodized salt, and purified water.',
        shelfLife: '12 Months from Packaging (kept continuously at -18°C)',
        allergens: 'CONTAINS SOY AND WHEAT (GLUTEN).',
        storage: 'Keep deep frozen at -18°C or below. Boil or marinate prior to cooking.',
        isNonFood: false,
        isFrozen: true,
      };
    }
    return {
      ingredients: '100% farm-picked green peas / sweet corn / diced carrots & beans — blanched and IQF frozen.',
      shelfLife: '12 Months from Packaging (kept continuously at -18°C)',
      allergens: 'No major allergens declared. Processed in a hygienic cold-chain facility.',
      storage: 'Keep frozen at -18°C or below at all times. Do not refreeze once thawed.',
      isNonFood: false,
      isFrozen: true,
    };
  }

  // Packaged Groceries
  if (/\batta\b/i.test(hay)) {
    return {
      ingredients: '100% Whole Wheat Grain (Chakki-ground MP Sharbati / whole wheat flour, zero maida added).',
      shelfLife: '4 to 6 Months from Packaging',
      allergens: 'CONTAINS WHEAT (GLUTEN).',
      storage: 'Transfer to an airtight container in a cool, dry place away from moisture.',
      isNonFood: false,
      isFrozen: false,
    };
  }
  if (/\b(sooji|maida|daliya|oats|corn flakes)\b/i.test(hay)) {
    return {
      ingredients: /\boats\b/i.test(hay)
        ? '100% Wholegrain Rolled Oats.'
        : /\bcorn\b/i.test(hay)
        ? 'Milled corn grits, sugar, malt extract, iodized salt, vitamins & minerals.'
        : 'Graded golden wheat / semolina (clean-milled).',
      shelfLife: '9 Months from Packaging',
      allergens: /\bcorn\b/i.test(hay) ? 'CONTAINS BARLEY MALT (GLUTEN). May contain wheat & almond traces.' : 'CONTAINS WHEAT / CEREAL GLUTEN.',
      storage: 'Keep tightly sealed in a cool, dry place after opening.',
      isNonFood: false,
      isFrozen: false,
    };
  }
  if (/\b(besan|dal|rajma|chana|moong)\b/i.test(hay) && !/\bhaldiram/i.test(hay)) {
    return {
      ingredients: /\bbesan\b/i.test(hay)
        ? '100% Pure Chana Dal (split chickpea) fine-ground gram flour.'
        : '100% Unpolished graded whole / split pulses & lentils, sortex cleaned.',
      shelfLife: '9 to 12 Months from Packaging',
      allergens: 'Zero major allergens. Naturally gluten-free pulse product.',
      storage: 'Store in an airtight jar away from humidity and direct sunlight.',
      isNonFood: false,
      isFrozen: false,
    };
  }
  if (/\brice\b/i.test(hay)) {
    return {
      ingredients: '100% Aged Long-Grain Basmati Rice, sortex cleaned & hygienically packed.',
      shelfLife: '18 to 24 Months from Packaging',
      allergens: 'Naturally Gluten-Free & Allergen-Free.',
      storage: 'Store in a dry pantry container away from heat and moisture.',
      isNonFood: false,
      isFrozen: false,
    };
  }
  if (/\bghee\b/i.test(hay)) {
    return {
      ingredients: '100% Clarified Milk Fat (Pure Desi Ghee crafted from fresh dairy cream/butter).',
      shelfLife: '9 Months from Packaging',
      allergens: 'CONTAINS MILK / DAIRY FAT.',
      storage: 'Keep lid tightly closed at room temperature away from moisture; no refrigeration needed.',
      isNonFood: false,
      isFrozen: false,
    };
  }
  if (/\b(oil|vanaspati)\b/i.test(hay)) {
    const isMustard = /\bmustard\b/i.test(hay);
    const isSoy = /\bsoy/i.test(hay);
    return {
      ingredients: isMustard
        ? '100% Cold-Pressed Kachi Ghani Mustard Seed Oil.'
        : isSoy
        ? 'Refined Soyabean Oil fortified with Vitamin A & Vitamin D2.'
        : 'Refined edible vegetable oil (Sunflower / Rice Bran / Palmolein) with permitted antioxidants & Vitamins A, D.',
      shelfLife: '9 to 12 Months from Packaging',
      allergens: isMustard
        ? 'Derived from Mustard Seeds. Allergen notice for mustard sensitivity.'
        : isSoy
        ? 'Derived from Soybeans (Refined Soyabean Oil).'
        : 'Free from milk, gluten, and peanuts.',
      storage: 'Keep tightly sealed in a cool, dark cabinet away from stove heat.',
      isNonFood: false,
      isFrozen: false,
    };
  }
  if (/\bsalt\b/i.test(hay)) {
    return {
      ingredients: /\brock\b/i.test(hay)
        ? '100% Natural Himalayan / Sendha Rock Salt.'
        : 'Refined Vacuum Evaporated Edible Iodized Salt, anti-caking agent (E536).',
      shelfLife: '24 Months from Packaging',
      allergens: 'Zero declared allergens.',
      storage: 'Store in a dry, moisture-free jar.',
      isNonFood: false,
      isFrozen: false,
    };
  }
  if (/\b(masala|haldi|mirch|coriander|pepper|methi|jeera|mustard seeds|ajwain|hing|herbs|mdh|everest|catch)\b/i.test(hay)) {
    return {
      ingredients: /\bhing\b/i.test(hay)
        ? 'Rice flour / edible gum, pure asafoetida resin, turmeric.'
        : '100% Graded whole spices / aromatic roasted & ground spices cleanly sealed.',
      shelfLife: '12 Months from Packaging',
      allergens: /\b(hing|sambhar|chana|garam|kitchen|mustard)\b/i.test(hay)
        ? 'Contains Mustard / Asafoetida (trace wheat starch in select compounded Hing).'
        : 'Naturally free of milk and nuts; packed in a spice facility.',
      storage: 'Keep in an airtight container away from steam to preserve aroma.',
      isNonFood: false,
      isFrozen: false,
    };
  }
  if (/\b(bournvita|horlicks|boost)\b/i.test(hay)) {
    return {
      ingredients: 'Malted barley extract, wheat flour, milk solids, sugar, cocoa solids, vitamins & essential minerals.',
      shelfLife: '12 Months from Packaging',
      allergens: 'CONTAINS BARLEY MALT, WHEAT (GLUTEN), AND MILK SOLIDS.',
      storage: 'Replace lid tightly after every scoop; store in a cool, dry place.',
      isNonFood: false,
      isFrozen: false,
    };
  }
  if (/\b(tea|chai|red label|taj mahal|wagh bakri|coffee|nescafe|bru|sugar|honey|rooh afza)\b/i.test(hay)) {
    return {
      ingredients: /\bcoffee\b/i.test(hay)
        ? 'Roasted Arabica/Robusta coffee beans (chicory blend in Bru Instant).'
        : /\bhoney\b/i.test(hay)
        ? '100% Pure Apiary Honey (Zero added sugar).'
        : /\bsugar\b/i.test(hay)
        ? '100% Double-refined sulphur-free cane sugar crystals.'
        : '100% Selected Assam & Darjeeling Tea Leaves (natural cardamom & ginger in spiced variant).',
      shelfLife: /\bhoney|sugar|coffee\b/i.test(hay) ? '18 to 24 Months from Packaging' : '12 Months from Packaging',
      allergens: 'Zero major allergens. Tea & Coffee contain natural caffeine.',
      storage: 'Store tightly sealed in a cool, dry place away from strong odors.',
      isNonFood: false,
      isFrozen: false,
    };
  }
  if (/\b(nutella|peanut butter|chocolate|dairy milk|dark fantasy|oreo|bourbon|good day|milk bikis|marie|parle)\b/i.test(hay)) {
    return {
      ingredients: /\bpeanut\b/i.test(hay)
        ? '100% Roasted Whole Peanuts (All-Natural).'
        : /\bnutella\b/i.test(hay)
        ? 'Sugar, palm oil, hazelnuts (13%), skimmed milk powder, fat-reduced cocoa, soy lecithin, vanillin.'
        : 'Refined wheat flour (maida), sugar, edible vegetable oil / butter, milk solids, cocoa solids, raising agents.',
      shelfLife: /\bpeanut\b/i.test(hay) ? '12 Months from Packaging' : '6 to 9 Months from Packaging',
      allergens: /\bpeanut\b/i.test(hay)
        ? 'CONTAINS PEANUTS.'
        : /\bnutella\b/i.test(hay)
        ? 'CONTAINS HAZELNUTS (TREE NUTS), MILK & SOY.'
        : 'CONTAINS WHEAT (GLUTEN), MILK SOLIDS & SOY. May contain traces of cashew/almond.',
      storage: 'Store in a cool, dry place; keep pack tightly sealed after opening.',
      isNonFood: false,
      isFrozen: false,
    };
  }
  if (/\b(maggi|yippee|chings|noodles)\b/i.test(hay) && !/\bsauce\b/i.test(hay)) {
    return {
      ingredients: 'Refined wheat flour (maida), palm oil, iodized salt, dehydrated onion/garlic spices, masala tastemaker.',
      shelfLife: '9 Months from Packaging',
      allergens: 'CONTAINS WHEAT (GLUTEN) & SOY. May contain traces of milk powder and mustard.',
      storage: 'Store in a cool, dry, clean place away from strong odors.',
      isNonFood: false,
      isFrozen: false,
    };
  }
  if (/\b(ketchup|sauce|jam)\b/i.test(hay)) {
    return {
      ingredients: /\bjam\b/i.test(hay)
        ? 'Mixed fruit pulp (apple, papaya, mango, pineapple), sugar, pectin, citric acid.'
        : 'Tomato paste / pulp, sugar, iodized salt, natural vinegar, mixed spices & permitted stabilizer.',
      shelfLife: '12 Months from Packaging',
      allergens: 'Zero major allergens. Refrigerate after opening.',
      storage: 'Store unopened bottle cool & dry; refrigerate after opening.',
      isNonFood: false,
      isFrozen: false,
    };
  }
  if (/\b(haldiram|lays|kurkure|uncle chipps)\b/i.test(hay)) {
    return {
      ingredients: 'Potatoes / chickpea flour / moong lentil / rice meal, edible vegetable oil, iodized salt & Indian masala spices.',
      shelfLife: '4 to 6 Months (Nitrogen-flushed freshness pack)',
      allergens: 'Contains trace Gluten / Milk Solids / Peanuts depending on spice seasoning.',
      storage: 'Store sealed away from direct sun and consume crisp upon opening.',
      isNonFood: false,
      isFrozen: false,
    };
  }

  return {
    ingredients: 'Factory-sealed FSSAI-compliant pantry grocery item.',
    shelfLife: '9 to 12 Months from Packaging',
    allergens: 'Check printed manufacturer label for trace allergen notes.',
    storage: 'Store in a cool, dry, hygienic pantry.',
    isNonFood: false,
    isFrozen,
  };
}

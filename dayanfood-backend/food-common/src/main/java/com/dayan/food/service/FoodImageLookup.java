package com.dayan.food.service;
/** Resolves a visible food image under the current caller's identity. */
public interface FoodImageLookup { String imageUrl(Long foodId); }

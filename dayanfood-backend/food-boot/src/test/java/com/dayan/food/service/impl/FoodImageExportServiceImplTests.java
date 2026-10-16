package com.dayan.food.service.impl;

import com.dayan.food.entity.vo.ImageExportVO;
import com.dayan.food.service.FoodImageLookup;
import com.dayan.food.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodImageExportServiceImplTests {

    private static final long FOOD_ID = 8L;

    @Mock
    private FoodImageLookup foodService;

    @Mock
    private ImageStorageService imageStorageService;

    private FoodImageExportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FoodImageExportServiceImpl(foodService, imageStorageService);
    }

    @Test
    void readsTheFoodImageFromInternalUploadStorage() {
        String imageUrl = ("/uploads/dish.png");
        ImageExportVO expected = new ImageExportVO(new byte[]{1, 2, 3}, "image/png");
        when(foodService.imageUrl(FOOD_ID)).thenReturn(imageUrl);
        when(imageStorageService.readForExport("/uploads/dish.png")).thenReturn(expected);

        ImageExportVO actual = service.read(FOOD_ID);

        assertEquals(expected, actual);
        verify(imageStorageService).readForExport("/uploads/dish.png");
    }

    @Test
    void rejectsNonSystemImageAddresses() {
        String imageUrl = ("https://images.example.test/dish.png");
        when(foodService.imageUrl(FOOD_ID)).thenReturn(imageUrl);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.read(FOOD_ID));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(imageStorageService);
    }

    @Test
    void reportsFoodsWithoutImages() {
        String imageUrl = (null);
        when(foodService.imageUrl(FOOD_ID)).thenReturn(imageUrl);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.read(FOOD_ID));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(imageStorageService);
    }

}

package ru.practicum.ewm.location.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.location.dto.LocationDto;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.location.repository.LocationRepository;
import ru.practicum.ewm.location.mapper.LocationMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private LocationMapper mapper;

    @InjectMocks
    private LocationService locationService;

    private LocationDto locationDto;
    private Location location;

    @BeforeEach
    void setUp() {
        locationDto = new LocationDto(55.7558f, 37.6176f);

        location = new Location(1L,55.7558f,37.6176f);
    }

    @Test
    void getOrCreateLocation_ExistingLocation_ShouldReturnExisting() {
        when(locationRepository.findByLatAndLon(55.7558f, 37.6176f))
                .thenReturn(Optional.of(location));

        Location result = locationService.getOrCreateLocation(locationDto);

        assertNotNull(result);
        assertEquals(location.getId(), result.getId());
        assertEquals(location.getLat(), result.getLat());
        assertEquals(location.getLon(), result.getLon());

        verify(locationRepository).findByLatAndLon(55.7558f, 37.6176f);
        verify(locationRepository, never()).save(any());
        verify(mapper, never()).toLocation(any());
    }

    @Test
    void getOrCreateLocation_NewLocation_ShouldCreateAndReturn() {
        Location newLocation = new Location(null,55.7558f,37.6176f);

        when(locationRepository.findByLatAndLon(55.7558f, 37.6176f))
                .thenReturn(Optional.empty());
        when(mapper.toLocation(locationDto)).thenReturn(newLocation);
        when(locationRepository.save(newLocation)).thenReturn(location);

        Location result = locationService.getOrCreateLocation(locationDto);

        assertNotNull(result);
        assertEquals(location.getId(), result.getId());
        assertEquals(location.getLat(), result.getLat());
        assertEquals(location.getLon(), result.getLon());

        verify(locationRepository).findByLatAndLon(55.7558f, 37.6176f);
        verify(mapper).toLocation(locationDto);
        verify(locationRepository).save(newLocation);
    }

    @Test
    void getOrCreateLocation_DifferentCoordinates_ShouldCreateNew() {
        LocationDto differentLocationDto = new LocationDto(59.9311f, 30.3609f);

        Location differentLocation = new Location(null,59.9311f,30.3609f);

        Location savedLocation = new Location(2L,59.9311f,30.3609f);

        when(locationRepository.findByLatAndLon(59.9311f, 30.3609f))
                .thenReturn(Optional.empty());
        when(mapper.toLocation(differentLocationDto)).thenReturn(differentLocation);
        when(locationRepository.save(differentLocation)).thenReturn(savedLocation);

        Location result = locationService.getOrCreateLocation(differentLocationDto);

        assertNotNull(result);
        assertEquals(savedLocation.getId(), result.getId());
        assertEquals(59.9311f, result.getLat());
        assertEquals(30.3609f, result.getLon());

        verify(locationRepository).findByLatAndLon(59.9311f, 30.3609f);
        verify(mapper).toLocation(differentLocationDto);
        verify(locationRepository).save(differentLocation);
    }

    @Test
    void getOrCreateLocation_ZeroCoordinates_ShouldWork() {
        LocationDto zeroLocationDto = new LocationDto(0.0f, 0.0f);

        Location zeroLocation = new Location(null,0.0f,0.0f);

        Location savedZeroLocation = new Location(4L,0.0f,0.0f);

        when(locationRepository.findByLatAndLon(0.0f, 0.0f))
                .thenReturn(Optional.empty());
        when(mapper.toLocation(zeroLocationDto)).thenReturn(zeroLocation);
        when(locationRepository.save(zeroLocation)).thenReturn(savedZeroLocation);

        Location result = locationService.getOrCreateLocation(zeroLocationDto);

        assertNotNull(result);
        assertEquals(savedZeroLocation.getId(), result.getId());
        assertEquals(0.0f, result.getLat());
        assertEquals(0.0f, result.getLon());

        verify(locationRepository).findByLatAndLon(0.0f, 0.0f);
        verify(mapper).toLocation(zeroLocationDto);
        verify(locationRepository).save(zeroLocation);
    }

    @Test
    void getOrCreateLocation_SameCoordinatesMultipleCalls_ShouldReturnSameLocation() {
        when(locationRepository.findByLatAndLon(55.7558f, 37.6176f))
                .thenReturn(Optional.of(location));

        Location result1 = locationService.getOrCreateLocation(locationDto);
        Location result2 = locationService.getOrCreateLocation(locationDto);

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.getId(), result2.getId());
        assertEquals(result1.getLat(), result2.getLat());
        assertEquals(result1.getLon(), result2.getLon());

        verify(locationRepository, times(2)).findByLatAndLon(55.7558f, 37.6176f);
        verify(locationRepository, never()).save(any());
        verify(mapper, never()).toLocation(any());
    }
}

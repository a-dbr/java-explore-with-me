package ru.practicum.ewm.compilation.mapper;

import org.mapstruct.Mapper;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.event.model.Event;

@Mapper(componentModel = "spring")
public interface CompilationMapper {

    Compilation toCompilation(NewCompilationDto dto);

    CompilationDto toCompilationDto(Compilation compilation);

    Event map(Long id);
}

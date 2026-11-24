package com.ecommerce.user.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.ecommerce.user.dto.Dtos.UserResponse;
import com.ecommerce.user.entity.User;

/**
 * MapStruct writes the code of this interface for us at compile time
 * (see target/generated-sources/annotations/.../UserMapperImpl.java).
 * Fields with the same name are copied; the enum Role is converted to a String automatically.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    List<UserResponse> toResponses(List<User> users);
}

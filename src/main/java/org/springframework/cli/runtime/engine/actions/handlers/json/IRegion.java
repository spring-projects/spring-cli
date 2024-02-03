package org.springframework.cli.runtime.engine.actions.handlers.json;

/**
 * Mimicks eclipse IRegion (i.e. a region is a offset + length).
 */
public interface IRegion {

    int getOffset();

    int getLength();

}

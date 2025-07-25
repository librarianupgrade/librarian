package org.locationtech.spatial4j;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// NOTE: we keep the header as it came from ASF; it did not originate in Spatial4j

import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.SpatialRelation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A predicate that compares a stored geometry to a supplied geometry. It's enum-like. For more
 * explanation of each predicate, consider looking at the source implementation
 * of {@link #evaluate(org.locationtech.spatial4j.shape.Shape, org.locationtech.spatial4j.shape.Shape)}. It's important
 * to be aware that Lucene-spatial makes no distinction of shape boundaries, unlike many standardized
 * definitions. Nor does it make dimensional distinctions (e.g. line vs polygon).
 * You can lookup a predicate by "Covers" or "Contains", for example, and you will get the
 * same underlying predicate implementation.
 *
 * @see <a href="http://en.wikipedia.org/wiki/DE-9IM">DE-9IM at Wikipedia, based on OGC specs</a>
 * @see <a href="http://edndoc.esri.com/arcsde/9.1/general_topics/understand_spatial_relations.htm">
 *   ESRIs docs on spatial relations</a>
 */
public abstract class SpatialPredicate implements Serializable {
	//TODO? Use enum?  LUCENE-5771

	// Private registry
	private static final Map<String, SpatialPredicate> registry = new HashMap<>();//has aliases
	private static final List<SpatialPredicate> list = new ArrayList<>();

	// Geometry Operations

	/** Bounding box of the *indexed* shape, then {@link #Intersects}. */
	public static final SpatialPredicate BBoxIntersects = new SpatialPredicate("BBoxIntersects") {
		@Override
		public boolean evaluate(Shape indexedShape, Shape queryShape) {
			return indexedShape.getBoundingBox().relate(queryShape).intersects();
		}
	};
	/** Bounding box of the *indexed* shape, then {@link #IsWithin}. */
	public static final SpatialPredicate BBoxWithin = new SpatialPredicate("BBoxWithin") {
		{
			register("BBoxCoveredBy");//alias -- the better name
		}

		@Override
		public boolean evaluate(Shape indexedShape, Shape queryShape) {
			Rectangle bbox = indexedShape.getBoundingBox();
			return bbox.relate(queryShape) == SpatialRelation.WITHIN || bbox.equals(queryShape);
		}
	};
	/** Meets the "Covers" OGC definition (boundary-neutral). */
	public static final SpatialPredicate Contains = new SpatialPredicate("Contains") {
		{
			register("Covers");//alias -- the better name
		}

		@Override
		public boolean evaluate(Shape indexedShape, Shape queryShape) {
			return indexedShape.relate(queryShape) == SpatialRelation.CONTAINS || indexedShape.equals(queryShape);
		}
	};
	/** Meets the "Intersects" OGC definition. */
	public static final SpatialPredicate Intersects = new SpatialPredicate("Intersects") {
		@Override
		public boolean evaluate(Shape indexedShape, Shape queryShape) {
			return indexedShape.relate(queryShape).intersects();
		}
	};
	/** Meets the "Equals" OGC definition. */
	public static final SpatialPredicate IsEqualTo = new SpatialPredicate("Equals") {
		{
			register("IsEqualTo");//alias (deprecated)
		}

		@Override
		public boolean evaluate(Shape indexedShape, Shape queryShape) {
			return indexedShape.equals(queryShape);
		}
	};
	/** Meets the "Disjoint" OGC definition. */
	public static final SpatialPredicate IsDisjointTo = new SpatialPredicate("Disjoint") {
		{
			register("IsDisjointTo");//alias (deprecated)
		}

		@Override
		public boolean evaluate(Shape indexedShape, Shape queryShape) {
			return !indexedShape.relate(queryShape).intersects();
		}
	};
	/** Meets the "CoveredBy" OGC definition (boundary-neutral). */
	public static final SpatialPredicate IsWithin = new SpatialPredicate("Within") {
		{
			register("IsWithin");//alias (deprecated)
			register("CoveredBy");//alias -- the more appropriate name.
		}

		@Override
		public boolean evaluate(Shape indexedShape, Shape queryShape) {
			return indexedShape.relate(queryShape) == SpatialRelation.WITHIN || indexedShape.equals(queryShape);
		}
	};
	/** Almost meets the "Overlaps" OGC definition, but boundary-neutral (boundary==interior). */
	public static final SpatialPredicate Overlaps = new SpatialPredicate("Overlaps") {
		@Override
		public boolean evaluate(Shape indexedShape, Shape queryShape) {
			return indexedShape.relate(queryShape) == SpatialRelation.INTERSECTS;//not Contains or Within or Disjoint
		}
	};

	private final String name;

	protected SpatialPredicate(String name) {
		this.name = name;
		register(name);
		list.add(this);
	}

	protected void register(String name) {
		registry.put(name, this);
		registry.put(name.toUpperCase(Locale.ROOT), this);
	}

	public static SpatialPredicate get(String v) {
		SpatialPredicate op = registry.get(v);
		if (op == null) {
			op = registry.get(v.toUpperCase(Locale.ROOT));
		}
		if (op == null) {
			throw new IllegalArgumentException("Unknown Operation: " + v);
		}
		return op;
	}

	public static List<SpatialPredicate> values() {
		return list;
	}

	public static boolean is(SpatialPredicate op, SpatialPredicate... tst) {
		for (SpatialPredicate t : tst) {
			if (op == t) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns whether the relationship between indexedShape and queryShape is
	 * satisfied by this operation.
	 */
	public abstract boolean evaluate(Shape indexedShape, Shape queryShape);

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}
}
